import asyncio
import logging
import sys

# Configure logging to print debug/error messages to stdout
logging.basicConfig(level=logging.DEBUG, stream=sys.stdout)

from app.services.mistral_service import analyze_feedback_weaknesses, moderate_project_content

async def main():
    print("=== Testing analyze_feedback_weaknesses ===")
    res_feedback = await analyze_feedback_weaknesses(
        "Code của bạn ấy có nhiều bug, đặc biệt là phần React component render lại liên tục làm giao diện bị giật lag."
    )
    print(f"Feedback Analysis Result: {res_feedback}")
    
    print("\n=== Testing moderate_project_content (Clean Project) ===")
    res_mod_clean = await moderate_project_content(
        title="Dự án phát triển ứng dụng di động HiBuddy giúp kết nối sinh viên học nhóm",
        description="Chúng mình là nhóm sinh viên CNTT đang xây dựng ứng dụng HiBuddy nhằm giúp các bạn sinh viên tìm kiếm bạn học cùng tiến, trao đổi bài tập và chia sẻ tài liệu học tập. Dự án phi lợi nhuận, rất mong tìm được 1 bạn thiết kế UI/UX đồng hành."
    )
    print(f"Clean Moderation Result: {res_mod_clean}")

asyncio.run(main())
