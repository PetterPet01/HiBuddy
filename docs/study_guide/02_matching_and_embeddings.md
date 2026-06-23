# Study Guide 02: Hybrid Matching and Semantic Vector Embeddings

This module describes how HiBuddy's core core-matching recommendation engine bridges deterministic heuristics (rule-based evaluation) with AI-powered semantic similarity. 

---

## 📐 1. The 100-Point Deterministic Match Scoring Algorithm

Before leveraging artificial intelligence, HiBuddy filters and ranks potentials using a strict multi-criteria scoring algorithm defined in `backend/app/services/matching_service.py`.

The algorithm is split depending on whether the system is ranking a **Project for a Contributor** or ranking a **Contributor for a Project**.

### Core Calculation Metrics (Project-for-Contributor: Score out of 100)

$$\text{Final Score} = (\text{Role Fit Score} \times 0.82) + (\text{Interest Intersection} \times 0.05) + (\text{Commitment Alignment} \times 0.05) + (\text{Owner Reputation} \times 0.05) + (\text{Recency Multiplier} \times 0.03)$$

1.  **Role Fit Score (82% Weight)**: This is the most crucial criteria. It evaluates how well the user's mapped roles match the vacancies in the project:
    *   **Role Mapping (70% of Role Fit)**: If the user's selected role matches an open `ProjectRoleSlot`, they receive points.
    *   **Skill Requirements (25% of Role Fit)**: Evaluates user skills against role requirements:
        *   Matched skill with required level $\implies 100\%$ score.
        *   Matched skill but user is one level lower (e.g., intermediate skill vs advanced requirement) $\implies 50\%$ score.
        *   Unmatched required skill $\implies 0\%$ score.
    *   **Slot Availability (5% of Role Fit)**: Prioritizes slots that have higher remaining capacity.
2.  **Interest Jaccard Index (5% Weight)**: Compares the overlap of a user’s interests with the project’s fields using a standard **Jaccard Similarity Coefficient**:
    $$\text{Jaccard} = \frac{|A \cap B|}{|A \cup B|}$$
3.  **Commitment Level Alignment (5% Weight)**: Compares requested hours/week. Direct matches get maximum points; minor differences get scaled down.
4.  **Owner Reputation (5% Weight)**: Incorporates the rating of the project creator (computed from post-project evaluations) to prioritize highly-rated team leaders.
5.  **Recency Multiplier (3% Weight)**: Slightly boosts newly created projects to prevent cold-start starvation.

---

## 🧠 2. Semantic NLP Vector Embedding Pipelines

Deterministic filtering fails when users write bios or project goals in free-form natural language using different keywords (e.g., "AI enthusiast" vs. "Machine learning developer"). To solve this, HiBuddy implements semantic vector matching under `backend/app/services/embedding_service.py`.

### The PyTorch Sentence-Transformers Pipeline
1.  **Model Loading**: During startup (`backend/app/main.py`), PyTorch initializes the **`all-MiniLM-L6-v2`** SentenceTransformer model. This lightweight yet highly performant model maps arbitrary text blocks into a **384-dimensional dense vector space**.
2.  **Paragraph Generation**: Structured profile parameters are transformed into highly contextualized English paragraphs:
    *   *User Text Builder*: Maps user roles, bios, and specific skills into a paragraph: 
        > *"Candidate desires Frontend role. Bio: Highly passionate about beautiful web interfaces and user experience. Skills: React (Intermediate), Jetpack Compose (Beginner)."*
    *   *Project Text Builder*: Maps project details, descriptions, and desired roles:
        > *"Project looking for Android UI developer. Description: An interactive platform for matching students. Demanded skills: Jetpack Compose, Kotlin."*
3.  **Inference**: The textual paragraph is fed into PyTorch, yielding a Python list of 384 floating-point coordinates representing the semantic context of that profile.

---

## ⚡ 3. High-Performance Milvus Vector Indexing

Once vectors are generated, they are indexed and queried in **Milvus**, a distributed vector database, via `backend/app/milvus_client.py`.

### Milvus Collection Setup
*   **Collections**: Two collections exist: `user_profile_vectors` and `project_vectors`.
*   **Metric Type**: **Cosine Similarity** (`MetricType.COSINE`) is used. Since vectors are normalized, cosine distance matches natural language context regardless of length.
*   **Index Type**: **HNSW (Hierarchical Navigable Small World)** is used with configurations:
    *   `M=16`: Maximum number of connection edges per node in the graph layers.
    *   `efConstruction=64`: Size of the dynamic candidate list evaluated during index construction.
    *   *Why HNSW?* HNSW provides logarithmic search time complexity $O(\log N)$, allowing immediate candidate retrieval even if the system scales to millions of student profiles.

### The Hybrid Matching Process (`backend/app/services/swipe_service.py`)
When a user requests recommendations (`_discover_projects`):
1.  The system pulls the logged-in user's profile text and generates its vector embedding.
2.  It sends a gRPC vector search query to the `project_vectors` collection in Milvus to find the top $K$ closest projects.
3.  **Relational Filtering**: Milvus allows boolean filter expressions. The backend injects filters to exclude:
    *   Projects the user has already swiped on (`LIKE` or `PASS`).
    *   Projects owned by the user themselves.
    *   Projects owned by users who have blocked the active user (or vice versa).
4.  **Hybrid Combination**: The final candidate pool is ranked by combining the semantic similarity and the deterministic scores:
    $$\text{Hybrid Score} = \frac{\text{Milvus Cosine Score} + \text{Deterministic Score}}{2}$$
    This guarantees that matches are both *semantically relevant* and *technically compliant* with skill/role requirements.

---

## 👨‍🏫 Lecturer Interview Prep: Q&A Focus

### Q1: "Why did you choose a Hybrid Matching model instead of relying 100% on AI embeddings?"
*   **Theoretical Answer**: Relying solely on AI embeddings can result in **semantic drift and technical mismatch**. For example, a user with a biography saying "I love building Android apps in Kotlin" will have a very high cosine similarity with a Kotlin-based mobile project. However, if the project specifically needs a "Senior Backend Developer with FastAPI skills" (which the user doesn't possess), the match is technically invalid. A hybrid model ensures that we filter by *hard criteria* (specific vacant slots, roles, and skills) while using *semantic search* to match soft criteria like interest contexts and descriptive bios.
*   **Code Reference**: Open `backend/app/services/swipe_service.py` (`_discover_projects` method, lines 898–978). Show the lecturer how the code fetches candidates from Milvus, computes the deterministic score, averages them on line 944 (`(vector_similarity + matching_score) / 2`), and sorts the final card stack.

### Q2: "What is HNSW, and why is Cosine Metric preferred over L2 (Euclidean) Distance for text embeddings?"
*   **Theoretical Answer**: **HNSW** is a graph-based indexing algorithm that constructs a multi-layered graph of vectors, allowing rapid approximate nearest neighbor (ANN) searches. **Cosine Metric** measures the angular similarity between two vectors, ranging from -1 to 1. **L2 (Euclidean) Distance** measures the physical distance between points, which is highly sensitive to text length. A long biography will contain more keywords and yield a vector with a larger magnitude than a short, concise biography, causing L2 distance to make them seem far apart even if they describe the exact same goals. Cosine similarity normalizes the vector lengths and looks strictly at the *direction* of the vectors, focusing purely on semantic alignment.
*   **Code Reference**: Refer to `backend/app/milvus_client.py` (Lines 56–103). Point out `MetricType.COSINE` and the indexing configuration dictionary showing `index_type: "HNSW"`.

---

*Move on to the next section: **[03. Real-Time Communication & Chat State Sync](03_realtime_websocket_and_chat.md)** to see how real-time components operate.*
