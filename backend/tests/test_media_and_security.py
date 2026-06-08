import io

from PIL import Image

from app.api.upload import _normalize_image
from app.core.security import (
    create_access_token,
    create_refresh_token,
    decode_token,
    generate_numeric_code,
    hash_token,
)


def test_avatar_normalization_produces_square_jpeg():
    source = Image.new("RGB", (1600, 800), color=(20, 80, 120))
    buffer = io.BytesIO()
    source.save(buffer, format="PNG")

    normalized = _normalize_image(
        buffer.getvalue(), (512, 512), crop_to_size=True
    )

    with Image.open(io.BytesIO(normalized)) as result:
        assert result.format == "JPEG"
        assert result.size == (512, 512)


def test_access_and_refresh_tokens_have_distinct_claims():
    access = decode_token(create_access_token("user-1"))
    refresh = decode_token(
        create_refresh_token("user-1", jti="jti-1", family="family-1")
    )

    assert access["type"] == "access"
    assert refresh["type"] == "refresh"
    assert refresh["jti"] == "jti-1"
    assert refresh["family"] == "family-1"


def test_codes_and_token_hashes_are_safe_to_store():
    code = generate_numeric_code()
    assert len(code) == 6
    assert code.isdigit()
    assert hash_token("secret-token") != "secret-token"
