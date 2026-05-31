#!/usr/bin/env python
"""
Interactive CLI Tool to test the Mistral AI Service.
This script can test both general conversation and the specific backend service functions:
- Peer Feedback Weakness Analysis
- Project Content Moderation
"""

import os
import sys
import asyncio
import json
from pathlib import Path

# Add the backend folder to sys.path so we can import the app modules
backend_dir = Path(__file__).resolve().parent
sys.path.append(str(backend_dir))

# Try to load environment variables using python-dotenv
try:
    from dotenv import load_dotenv
    load_dotenv(backend_dir / ".env")
except ImportError:
    # Fallback manual parsing if dotenv is not installed
    env_path = backend_dir / ".env"
    if env_path.exists():
        with open(env_path, "r", encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if line and not line.startswith("#") and "=" in line:
                    key, val = line.split("=", 1)
                    # Strip quotes if present
                    val_str = val.strip().strip("'\"")
                    os.environ[key.strip()] = val_str

# ANSI styling for premium terminal aesthetic
RESET = "\033[0m"
BOLD = "\033[1m"
UNDERLINE = "\033[4m"

# Colors
GREEN = "\033[38;2;46;204;113m"
CYAN = "\033[38;2;52;152;219m"
YELLOW = "\033[38;2;241;196;15m"
RED = "\033;2;231;76;60m"
PURPLE = "\033[38;2;155;89;182m"
DARK_GRAY = "\033[38;2;127;140;141m"
LIGHT_GRAY = "\033[38;2;220;220;220m"

def print_header(title: str):
    print(f"\n{BOLD}{PURPLE}================================================================{RESET}")
    print(f"{BOLD}{PURPLE} ***  {title.upper()}  ***{RESET}")
    print(f"{BOLD}{PURPLE}================================================================{RESET}")

def print_sub_header(title: str):
    print(f"\n{BOLD}{CYAN}--- {title} ---{RESET}")

def print_success(msg: str):
    print(f"{BOLD}{GREEN}[SUCCESS] {msg}{RESET}")

def print_error(msg: str):
    print(f"{BOLD}{RED}[ERROR] {msg}{RESET}")

def print_info(msg: str):
    print(f"{CYAN}[INFO] {msg}{RESET}")

def print_debug(msg: str):
    print(f"{DARK_GRAY}[DEBUG] {msg}{RESET}")


# Verify imports of httpx and app service
try:
    import httpx
except ImportError:
    print_error("Library 'httpx' is required. Please install it with: pip install httpx")
    sys.exit(1)

# Import the actual service functions to test their functionality
try:
    from app.services.mistral_service import (
        analyze_feedback_weaknesses,
        moderate_project_content,
        _call_mistral
    )
    from app.config import get_settings
    service_imported = True
except ImportError as e:
    service_imported = False
    print_debug(f"Could not import app.services.mistral_service directly: {e}")
    print_debug("Using direct API communication logic instead.")


async def test_env_config():
    """Verify and print environment configuration."""
    print_sub_header("Environment Configuration Check")

    api_key = os.getenv("MISTRAL_API_KEY")
    model = os.getenv("MISTRAL_MODEL", "ministral-3b-2510")

    if service_imported:
        try:
            settings = get_settings()
            api_key = settings.MISTRAL_API_KEY
            model = settings.MISTRAL_MODEL
            print_success("Successfully imported application configuration settings!")
        except Exception as e:
            print_error(f"Error loading app settings: {e}")

    if not api_key:
        print_error("MISTRAL_API_KEY is not defined in your environment or backend/.env file!")
        print_info("Please edit 'backend/.env' and add: MISTRAL_API_KEY=your_key_here")
        return False

    masked_key = api_key[:4] + "..." + api_key[-4:] if len(api_key) > 8 else "********"
    print_success(f"Mistral API Key Found: {BOLD}{masked_key}{RESET}")
    print_success(f"Mistral Model: {BOLD}{model}{RESET}")
    return True


async def direct_chat_loop():
    """General conversation chat loop with Mistral."""
    print_header("General Chat Mode")
    print_info("Type '/exit' or '/quit' to return to the main menu.")
    print_info("You are chatting with Mistral. Ask it anything!")

    api_key = os.getenv("MISTRAL_API_KEY") or (get_settings().MISTRAL_API_KEY if service_imported else None)
    model = os.getenv("MISTRAL_MODEL", "ministral-3b-2510") or (get_settings().MISTRAL_MODEL if service_imported else None)
    endpoint = "https://mistral.24102006.xyz/v1/chat/completions"

    chat_history = []

    while True:
        try:
            user_input = input(f"\n{BOLD}{GREEN}You: {RESET}").strip()
            if not user_input:
                continue

            if user_input.lower() in ["/exit", "/quit"]:
                break

            print(f"{DARK_GRAY}Thinking...{RESET}", end="\r")

            # Form conversation messages
            messages = [{"role": "system", "content": "You are a helpful and intelligent AI assistant."}]
            # Add history
            for speaker, text in chat_history:
                messages.append({"role": speaker, "content": text})
            # Add current user prompt
            messages.append({"role": "user", "content": user_input})

            async with httpx.AsyncClient(timeout=45.0) as client:
                response = await client.post(
                    endpoint,
                    headers={
                        "Authorization": f"Bearer {api_key}",
                        "Content-Type": "application/json",
                    },
                    json={
                        "model": model,
                        "messages": messages,
                        "temperature": 0.7,
                        "max_tokens": 800,
                    },
                )
                response.raise_for_status()
                result_json = response.json()
                reply = result_json["choices"][0]["message"]["content"].strip()

                # Clear "Thinking..." line
                print(" " * 15, end="\r")

                print(f"{BOLD}{PURPLE}Mistral:{RESET} {reply}")

                # Save to history
                chat_history.append(("user", user_input))
                chat_history.append(("assistant", reply))

        except KeyboardInterrupt:
            print()
            break
        except Exception as e:
            # Clear "Thinking..." line
            print(" " * 15, end="\r")
            print_error(f"Error calling Mistral API: {e}")
            if 'response' in locals() and response is not None:
                try:
                    print_error(f"Response details: {response.text}")
                except Exception:
                    pass


async def test_feedback_analysis():
    """Interactive testing for peer feedback analyzer."""
    print_header("Peer Feedback Analyzer")
    print_info("This test replicates the exact prompt and processing used in HiBuddy's backend")
    print_info("to extract specific skill weaknesses from peer feedback (max 5 skills).")
    print_info("Type '/exit' to return to the main menu.")

    while True:
        try:
            feedback = input(f"\n{BOLD}{YELLOW}Enter peer feedback text to analyze:{RESET}\n> ").strip()
            if not feedback:
                continue

            if feedback.lower() in ["/exit", "/quit"]:
                break

            print(f"{DARK_GRAY}Analyzing feedback...{RESET}")

            if service_imported:
                # Call actual service function
                weaknesses = await analyze_feedback_weaknesses(feedback)
            else:
                # Fallback to manual execution of service logic if import failed
                system_prompt = """You are a skill gap analyzer. Given anonymous peer feedback about a team member, identify the specific technical or soft skills the person is weak at and should improve.

Rules:
- Return ONLY a JSON array of skill name strings.
- Extract concrete skills (e.g., "React", "Docker", "Communication").
- Max 5 skills. If none, return []."""

                api_key = os.getenv("MISTRAL_API_KEY")
                model = os.getenv("MISTRAL_MODEL", "ministral-3b-2510")

                async with httpx.AsyncClient(timeout=30.0) as client:
                    response = await client.post(
                        "https://mistral.24102006.xyz/v1/chat/completions",
                        headers={
                            "Authorization": f"Bearer {api_key}",
                            "Content-Type": "application/json",
                        },
                        json={
                            "model": model,
                            "messages": [
                                {"role": "system", "content": system_prompt},
                                {"role": "user", "content": f"Analyze this peer feedback:\n\n{feedback}"},
                            ],
                            "temperature": 0.1,
                            "max_tokens": 150,
                        },
                    )
                    response.raise_for_status()
                    content = response.json()["choices"][0]["message"]["content"].strip()
                    if content.startswith("```"):
                        content = content.split("\n", 1)[1].rsplit("\n", 1)[0]

                    try:
                        weaknesses = json.loads(content)
                    except Exception as e:
                        print_error(f"Raw reply was not valid JSON: {content}")
                        weaknesses = []

            print_sub_header("Analysis Results")
            if weaknesses:
                print_success(f"Detected {len(weaknesses)} Weaknesses / Skill Gaps:")
                for idx, skill in enumerate(weaknesses, 1):
                    print(f"  {idx}. {BOLD}{RED}{skill}{RESET}")
            else:
                print_success("No skill gaps / weaknesses detected. Excellent feedback!")

        except KeyboardInterrupt:
            print()
            break
        except Exception as e:
            print_error(f"Failed during feedback analysis: {e}")


async def test_moderation():
    """Interactive testing for content moderation."""
    print_header("Project Content Moderation")
    print_info("This test checks whether project titles and descriptions violate community standards.")
    print_info("Violation categories include: spam, fraud, offensive, provocative, hostile.")
    print_info("Type '/exit' to return to the main menu.")

    while True:
        try:
            print(f"\n{BOLD}{YELLOW}Enter details for the project to test:{RESET}")
            title = input("Project Title: ").strip()
            if title.lower() in ["/exit", "/quit"]:
                break

            description = input("Project Description: ").strip()
            if description.lower() in ["/exit", "/quit"]:
                break

            if not title or not description:
                print_error("Both Title and Description are required for verification.")
                continue

            print(f"{DARK_GRAY}Checking content moderation rules...{RESET}")

            if service_imported:
                # Call actual service function
                result = await moderate_project_content(title=title, description=description)
            else:
                # Fallback manual implementation
                system_prompt = """You are a content moderation assistant. Given a project posting (title, description, goals, requirements), determine if the content violates community standards.

Violation categories:
- spam: Promotional content, irrelevant links, repetitive content
- fraud: Scams, deceptive offers, false promises, impersonation
- offensive: Profanity, hate speech, discriminatory language, harassment
- provocative: Trolling, inflammatory content, baiting
- hostile: Threats, intimidation, violent content

Rules:
- Return ONLY a JSON object with this exact structure: {"is_flagged": bool, "categories": [string], "reasons": [string]}
- Be conservative."""

                project_text = f"Title: {title}\nDescription: {description}\n"
                api_key = os.getenv("MISTRAL_API_KEY")
                model = os.getenv("MISTRAL_MODEL", "ministral-3b-2510")

                async with httpx.AsyncClient(timeout=30.0) as client:
                    response = await client.post(
                        "https://mistral.24102006.xyz/v1/chat/completions",
                        headers={
                            "Authorization": f"Bearer {api_key}",
                            "Content-Type": "application/json",
                        },
                        json={
                            "model": model,
                            "messages": [
                                {"role": "system", "content": system_prompt},
                                {"role": "user", "content": f"Review this project posting for community standards violations:\n\n{project_text}"},
                            ],
                            "temperature": 0.1,
                            "max_tokens": 300,
                        },
                    )
                    response.raise_for_status()
                    content = response.json()["choices"][0]["message"]["content"].strip()
                    if content.startswith("```"):
                        content = content.split("\n", 1)[1].rsplit("\n", 1)[0]

                    try:
                        result = json.loads(content)
                    except Exception as e:
                        print_error(f"Raw reply was not valid JSON: {content}")
                        result = {"is_flagged": False, "categories": [], "reasons": []}

            print_sub_header("Moderation Result")
            is_flagged = result.get("is_flagged", False)

            if is_flagged:
                print_error("CONTENT FLAGGED! This post violates community standards.")
                print(f"  {BOLD}Violation Categories:{RESET} {', '.join(result.get('categories', []))}")
                print(f"  {BOLD}Reason(s):{RESET}")
                for reason in result.get("reasons", []):
                    print(f"    - {reason}")
            else:
                print_success("Content passed moderation. Clean and acceptable!")

        except KeyboardInterrupt:
            print()
            break
        except Exception as e:
            print_error(f"Failed during content moderation check: {e}")


async def main():
    # Set Windows terminal support for ANSI colors and UTF-8 output encoding
    if sys.platform == "win32":
        try:
            import ctypes
            kernel32 = ctypes.windll.kernel32
            kernel32.SetConsoleMode(kernel32.GetStdHandle(-11), 7)
        except Exception:
            pass
        try:
            # Force standard output/error to use UTF-8 to support Vietnamese and special characters safely
            sys.stdout.reconfigure(encoding="utf-8")
            sys.stderr.reconfigure(encoding="utf-8")
        except Exception:
            pass

    print_header("HiBuddy Mistral AI Testing Tool")

    # 1. Check env config
    config_ok = await test_env_config()
    if not config_ok:
        print_error("Please configure your .env file before proceeding.")
        # We still allow proceeding in case they set variables in terminal environment
        choice = input("\nDo you still want to proceed? (y/N): ").strip().lower()
        if choice != "y":
            sys.exit(1)

    if service_imported:
        print_success("Backend service codebase detected. Testing will use real codebase paths!")
    else:
        print_info("Running in standalone client mode. (Make sure your virtualenv has httpx installed)")

    while True:
        print_sub_header("Main Menu")
        print("1. [Chat] Start Interactive Chat (Free Conversation)")
        print("2. [Feedback] Test Peer Feedback Skill-Gap Analyzer")
        print("3. [Moderation] Test Project Content Moderation")
        print("4. [Config] Re-run Config/Key Check")
        print("5. [Exit] Exit")

        try:
            choice = input(f"\n{BOLD}Select an option (1-5): {RESET}").strip()

            if choice == "1":
                await direct_chat_loop()
            elif choice == "2":
                await test_feedback_analysis()
            elif choice == "3":
                await test_moderation()
            elif choice == "4":
                await test_env_config()
            elif choice == "5" or choice.lower() in ["exit", "quit", "q"]:
                print_info("Goodbye!")
                break
            else:
                print_error("Invalid option. Please enter a number between 1 and 5.")
        except KeyboardInterrupt:
            print_info("\nGoodbye!")
            break
        except Exception as e:
            print_error(f"An unexpected error occurred: {e}")


if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        pass
