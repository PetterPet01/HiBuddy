from pymilvus import connections, Collection, FieldSchema, CollectionSchema, DataType, utility
from app.config import get_settings

settings = get_settings()


def connect_milvus():
    connections.connect(
        alias="default",
        host=settings.MILVUS_HOST,
        port=settings.MILVUS_PORT,
    )


def disconnect_milvus():
    connections.disconnect("default")


def init_milvus_collections():
    connect_milvus()

    user_fields = [
        FieldSchema(name="id", dtype=DataType.INT64, is_primary=True, auto_id=True),
        FieldSchema(name="user_id", dtype=DataType.VARCHAR, max_length=36),
        FieldSchema(name="vector", dtype=DataType.FLOAT_VECTOR, dim=settings.EMBEDDING_DIM),
        FieldSchema(name="mode", dtype=DataType.VARCHAR, max_length=20),
        FieldSchema(name="is_active", dtype=DataType.BOOL),
        FieldSchema(name="reputation_score", dtype=DataType.FLOAT),
    ]
    user_schema = CollectionSchema(fields=user_fields, description="User profile vectors")

    if not utility.has_collection("user_profile_vectors"):
        collection = Collection(name="user_profile_vectors", schema=user_schema)
        index_params = {
            "metric_type": "COSINE",
            "index_type": "HNSW",
            "params": {"M": 16, "efConstruction": 200},
        }
        collection.create_index(field_name="vector", index_params=index_params)
        collection.create_index(field_name="mode", index_name="idx_mode")
        collection.create_index(field_name="is_active", index_name="idx_active")
        collection.load()

    project_fields = [
        FieldSchema(name="id", dtype=DataType.INT64, is_primary=True, auto_id=True),
        FieldSchema(name="project_id", dtype=DataType.VARCHAR, max_length=36),
        FieldSchema(name="vector", dtype=DataType.FLOAT_VECTOR, dim=settings.EMBEDDING_DIM),
        FieldSchema(name="field", dtype=DataType.VARCHAR, max_length=50),
        FieldSchema(name="status", dtype=DataType.VARCHAR, max_length=20),
        FieldSchema(name="work_mode", dtype=DataType.VARCHAR, max_length=20),
    ]
    project_schema = CollectionSchema(fields=project_fields, description="Project vectors")

    if not utility.has_collection("project_vectors"):
        collection = Collection(name="project_vectors", schema=project_schema)
        index_params = {
            "metric_type": "COSINE",
            "index_type": "HNSW",
            "params": {"M": 16, "efConstruction": 200},
        }
        collection.create_index(field_name="vector", index_params=index_params)
        collection.create_index(field_name="field", index_name="idx_field")
        collection.create_index(field_name="status", index_name="idx_status")
        collection.load()

    disconnect_milvus()


def get_milvus_collection(name: str) -> Collection:
    connect_milvus()
    collection = Collection(name=name)
    collection.load()
    return collection
