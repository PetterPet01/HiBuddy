# Feature List 02: Hybrid Matching and Semantic Vector Embeddings

This document expands upon Study Guide 02 by providing an exhaustive list of features supported by the matching algorithm and vector database integrations.

## 1. Deterministic Multi-Criteria Matching
*   **Role-Based Filtering:** Ensuring candidate roles match open project slots.
*   **Skill Level Validation:** Scoring candidates against minimum required skill levels.
*   **Interest Jaccard Index Calculation:** Determining overlap between candidate interests and project fields.
*   **Commitment Level Evaluation:** Scoring alignment between requested hours and candidate availability.
*   **Reputation Integration:** Factoring in historical evaluations of the project owner.
*   **Recency Boost:** Applying a modifier for newer projects to increase early visibility.
*   **Capacity Filtering:** Removing fully-staffed projects from candidate swiping decks.

## 2. Natural Language Processing (NLP) Vectorization
*   **PyTorch SentenceTransformer Pipeline:** Local inference using `all-MiniLM-L6-v2`.
*   **Profile Paragraph Generation:** Contextualizing structured user data (roles, bio, skills) into readable paragraphs for vectorization.
*   **Project Paragraph Generation:** Contextualizing project descriptions, goals, and role slots for vectorization.
*   **Background Embedding Updates:** Generating or refreshing vector embeddings asynchronously upon profile/project edits.

## 3. Milvus Vector Database Integration
*   **Collection Management:** Dedicated collections for `user_profile_vectors` and `project_vectors`.
*   **HNSW Indexing:** High-performance approximate nearest neighbor search structuring.
*   **Cosine Similarity Metric:** Calculating angular distance between semantic embeddings.
*   **Hybrid Query Execution:**
    *   Retrieving Top-K nearest neighbors from Milvus.
    *   Applying relational SQL filters (ignoring already swiped, owned, or blocked projects).
*   **Score Blending:** Averaging the Milvus Cosine score with the Deterministic Match Score to generate the final candidate ranking.
