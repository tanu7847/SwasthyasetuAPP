import os
from pymongo import MongoClient, ASCENDING, DESCENDING

MONGO_URI = os.getenv("MONGO_URI", "mongodb://localhost:27017")
DATABASE_NAME = os.getenv("DATABASE_NAME", "swasthyasetu")

client = MongoClient(
    MONGO_URI,
    serverSelectionTimeoutMS=5000,
    connectTimeoutMS=5000,
)
db = client[DATABASE_NAME]


def ensure_indexes():
    db.users.create_index([("email", ASCENDING)], unique=True)
    db.symptoms.create_index([("patient_id", ASCENDING), ("created_at", DESCENDING)])
    db.consultations.create_index([("patient_id", ASCENDING), ("created_at", DESCENDING)])
    db.consultations.create_index([("doctor_id", ASCENDING), ("created_at", DESCENDING)])
    db.transcripts.create_index([("consultation_id", ASCENDING), ("created_at", DESCENDING)])
    db.prescriptions.create_index([("patient_id", ASCENDING), ("updated_at", DESCENDING)])
    db.referrals.create_index([("patient_id", ASCENDING), ("created_at", DESCENDING)])
    db.uploads.create_index([("consultation_id", ASCENDING), ("created_at", DESCENDING)])
    db.medical_records.create_index([("patient_id", ASCENDING), ("created_at", DESCENDING)])
    db.audit_logs.create_index([("created_at", DESCENDING)])