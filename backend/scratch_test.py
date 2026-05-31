import httpx
import asyncio

async def test():
    url = "https://mistral.24102006.xyz/v1/chat/completions"
    
    # Test with ministral-3b-2510 model
    payload = {
        "model": "ministral-3b-2510",
        "messages": [
            {"role": "system", "content": "You are a helpful assistant."},
            {"role": "user", "content": "Hello"}
        ]
    }

    async with httpx.AsyncClient(timeout=10.0) as client:
        try:
            print("Testing ministral-3b-2510 model...")
            res = await client.post(url, json=payload, headers={"Content-Type": "application/json"})
            print(f"Status Code: {res.status_code}")
            print(f"Response: {res.text[:500]}")
        except Exception as e:
            print(f"Error: {e}")

asyncio.run(test())
