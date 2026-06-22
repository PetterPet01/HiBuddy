"""Shared fuzzy matching helpers for search and recommendation.

Provides skill/role alias normalization and fuzzy similarity so that a query
like "py" or "react" matches catalog entries such as "Python" or
"React.js" even without exact strings.
"""

import re

from rapidfuzz import fuzz


# Common skill aliases -> canonical catalog name (lowercased keys).
SKILL_ALIASES: dict[str, str] = {
    "py": "python",
    "python3": "python",
    "python programming": "python",
    "js": "javascript",
    "node": "node.js",
    "nodejs": "node.js",
    "node js": "node.js",
    "reactjs": "react",
    "react.js": "react",
    "ts": "typescript",
    "ml": "machine learning",
    "ai": "machine learning",
    "deep learning": "machine learning",
    "dl": "machine learning",
    "nlp": "natural language processing",
    "cv": "computer vision",
    "k8s": "kubernetes",
    "postgres": "postgresql",
    "psql": "postgresql",
    "gcp": "google cloud",
    "aws cloud": "aws",
    "ui": "ui/ux design",
    "ux": "ui/ux design",
    "ui ux": "ui/ux design",
    "ui/ux": "ui/ux design",
    "figma design": "figma",
    "golang": "go",
    "c sharp": "c#",
    "csharp": "c#",
    "cpp": "c++",
    "rest": "api design",
    "rest api": "api design",
    "restful api": "api design",
}


def normalize_skill(value: str) -> str:
    normalized = re.sub(r"\s+", " ", (value or "").strip().lower())
    return SKILL_ALIASES.get(normalized, normalized)


def fuzzy_score(query: str, candidate: str) -> float:
    """Return a 0-100 similarity between two skill/role strings.

    Aliases are resolved first, then token-set ratio handles word
    reordering and partial overlaps ("react developer" vs "react").
    """
    q = normalize_skill(query)
    c = normalize_skill(candidate)
    if not q or not c:
        return 0.0
    if q == c:
        return 100.0
    return float(fuzz.token_set_ratio(q, c))


def fuzzy_matches(query: str, candidate: str, threshold: float = 80.0) -> bool:
    return fuzzy_score(query, candidate) >= threshold


def best_fuzzy_score(query: str, candidates: list[str]) -> float:
    if not candidates:
        return 0.0
    return max(fuzzy_score(query, c) for c in candidates)
