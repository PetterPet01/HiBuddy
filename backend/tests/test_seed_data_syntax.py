import subprocess
import sys
from pathlib import Path


def test_seed_data_compiles():
    backend_dir = Path(__file__).resolve().parents[1]
    result = subprocess.run(
        [sys.executable, "-m", "py_compile", str(backend_dir / "seed_data.py")],
        capture_output=True,
        text=True,
    )

    assert result.returncode == 0, result.stderr
