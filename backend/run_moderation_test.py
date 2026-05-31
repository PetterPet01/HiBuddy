import asyncio
import logging
import sys

# Configure logging
logging.basicConfig(level=logging.DEBUG, stream=sys.stdout)

from app.services.mistral_service import moderate_project_content
from app.config import get_settings

async def main():
    settings = get_settings()
    print(f"MISTRAL_API_KEY: {repr(settings.MISTRAL_API_KEY)}")
    print(f"MISTRAL_MODEL: {repr(settings.MISTRAL_MODEL)}")
    
    print("\nRunning moderate_project_content...")
    res = await moderate_project_content(
        title="Dự án phát triển ứng dụng di động HiBuddy giúp kết nối sinh viên học nhóm",
        description="Chúng mình là nhóm sinh viên CNTT đang xây dựng ứng dụng HiBuddy nhằm giúp các bạn sinh viên tìm kiếm bạn học cùng tiến, trao đổi bài tập và chia sẻ tài liệu học tập. Dự án phi lợi nhuận, rất mong tìm được 1 bạn thiết kế UI/UX đồng hành."
    )
    print(f"\nResult: {res}")

asyncio.run(main())
