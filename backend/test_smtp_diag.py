import asyncio
import aiosmtplib
from email.message import EmailMessage

async def test_smtp():
    message = EmailMessage()
    message["From"] = "petyukinon@gmail.com"
    message["To"] = "petyukinon@gmail.com"
    message["Subject"] = "HiBuddy SMTP Test"
    message.set_content("This is a test email from HiBuddy.")
    
    try:
        print("Connecting to smtp.gmail.com:587...")
        await aiosmtplib.send(
            message,
            hostname="smtp.gmail.com",
            port=587,
            username="petyukinon@gmail.com",
            password="HiBuddy01!",
            start_tls=True,
        )
        print("Email sent successfully!")
    except Exception as e:
        print(f"Failed to send email: {type(e).__name__}: {e}")

asyncio.run(test_smtp())
