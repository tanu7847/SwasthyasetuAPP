
from datetime import datetime, timedelta, timezone

import bcrypt
from bson import ObjectId
from fastapi import Depends, HTTPException
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from jose import JWTError, jwt

from .config import settings
from .db import db

JWT_ALGORITHM = "HS256"
JWT_SECRET = settings.jwt_secret
TOKEN_MINUTES = settings.jwt_expire_minutes

bearer = HTTPBearer(auto_error=True)

if not JWT_SECRET or len(JWT_SECRET) < 32:
    raise RuntimeError(
        "JWT_SECRET must be configured in backend/.env "
        "and contain at least 32 characters."
    )


def hash_password(password: str) -> str:
    return bcrypt.hashpw(
        password.encode("utf-8"),
        bcrypt.gensalt(),
    ).decode("utf-8")


def verify_password(password: str, hashed: str) -> bool:
    try:
        return bcrypt.checkpw(
            password.encode("utf-8"),
            hashed.encode("utf-8"),
        )
    except (ValueError, TypeError, AttributeError):
        return False


def create_token(user_id: str, role: str) -> str:
    now = datetime.now(timezone.utc)

    payload = {
        "sub": str(user_id),
        "role": role,
        "iat": now,
        "exp": now + timedelta(minutes=TOKEN_MINUTES),
    }

    return jwt.encode(
        payload,
        JWT_SECRET,
        algorithm=JWT_ALGORITHM,
    )


def current_user(
    credentials: HTTPAuthorizationCredentials = Depends(bearer),
):
    try:
        payload = jwt.decode(
            credentials.credentials,
            JWT_SECRET,
            algorithms=[JWT_ALGORITHM],
        )

        user_id = payload.get("sub")
        print("DEBUG JWT subject:", repr(user_id))
        print("DEBUG JWT subject type:", type(user_id).__name__)
        if not isinstance(user_id, str) or not ObjectId.is_valid(user_id):
            raise HTTPException(
                status_code=401,
                detail="Invalid token subject.",
                headers={"WWW-Authenticate": "Bearer"},
            )

    except JWTError:
        raise HTTPException(
            status_code=401,
            detail="Invalid or expired token. Please log in again.",
            headers={"WWW-Authenticate": "Bearer"},
        )

    user = db.users.find_one(
        {"_id": ObjectId(user_id), "active": True},
        {"password_hash": 0},
    )

    if not user:
        raise HTTPException(
            status_code=401,
            detail="Account not found or inactive.",
            headers={"WWW-Authenticate": "Bearer"},
        )

    return user


def require_roles(*allowed_roles: str):
    def role_checker(user=Depends(current_user)):
        if user.get("role") not in allowed_roles:
            raise HTTPException(
                status_code=403,
                detail="You do not have permission to access this resource.",
            )
        return user

    return role_checker


def is_assigned(doctor_id, patient_id: str) -> bool:
    """Check whether a doctor is assigned to the specified patient."""
    doctor_id = str(doctor_id)
    patient_id = str(patient_id)

    doctor = db.users.find_one(
        {"_id": ObjectId(doctor_id), "role": "doctor"},
        {"assigned_patient_ids": 1},
    )

    if not doctor:
        return False

    assigned_ids = [
        str(value)
        for value in doctor.get("assigned_patient_ids", [])
    ]

    if patient_id in assigned_ids:
        return True

    # Also support the assignment collection used by the application.
    try:
        assignment = db.patient_assignments.find_one({
            "doctor_user_id": doctor_id,
            "patient_id": patient_id,
        })
        return assignment is not None
    except Exception:
        return False


def require_patient_or_assigned_doctor(
    user=Depends(current_user),
    patient_id: str = None,
):
    """Allow a patient to access their own record, or their assigned doctor."""
    if user.get("role") == "patient":
        if patient_id and str(user["_id"]) == str(patient_id):
            return user

        raise HTTPException(
            status_code=403,
            detail="You can only access your own patient record.",
        )

    if user.get("role") == "doctor":
        if patient_id and is_assigned(user["_id"], patient_id):
            return user

        raise HTTPException(
            status_code=403,
            detail="You are not assigned to this patient.",
        )

    raise HTTPException(
        status_code=403,
        detail="Access denied.",
    )
