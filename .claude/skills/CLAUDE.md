---
name: claude-efficiency
description: Instructions for Claude Code on how to operate efficiently and minimize token usage within this repository.
user-invocable: false
---

# Claude Runtime Efficiency & Cost Optimization

This guide ensures that Claude operates efficiently in this Chess-backend Spring Boot repository, saving context tokens and reducing costs while working on complex backend tasks.

## 1. Project Context
This is a **Clean Architecture Spring Boot WebSockets backend** for a Chess application. Do not drift from this context. Adhere to the established `clean-arch`, `clean-code`, and `modules` skills.

## 2. Token Saving Strategies (Context Management)
When working on large refactors or features in this repository, strictly adhere to these token-saving practices:

* **Clear Stale Context:** When switching to an unrelated task (e.g., moving from "Matchmaking" to "Auth"), use the `/clear` command to start a fresh context window. 
* **Use Compaction:** If the conversation history becomes large while debugging, use the `/compact` command. You can give specific instructions like `/compact Focus on test output and code changes` to preserve only what matters.
* **Rename and Resume:** Use `/rename` to label sessions before clearing them, allowing you to easily `/resume` them later instead of dumping all files into one mega-session.

## 3. Working Efficiently on Complex Tasks
* **Plan First:** When asked to build a new feature (like a Tournament Engine), enter **plan mode** (Shift+Tab for the user, or proposing a plan first before generating thousands of lines of code). Present a step-by-step approach and wait for approval to avoid expensive re-work.
* **Test Incrementally:** Write one file or layer at a time, test it, and verify it. Do not attempt to write 10 new files in a single pass.
* **Course Correct Early:** If you realize a solution is incorrect, suggest a `/rewind` to an earlier conversation state rather than filling the context with apologies and messy rollback code.

## 4. MCP Server Management
* **Prefer Native CLI Tools:** Use native CLI commands (e.g., standard `git`, `grep`, `mvn` or `powershell` equivalents) when possible, as they consume fewer tokens than invoking complex MCP server listings.
* **Suggest Disabling Unused Tools:** If there are MCP servers active that are irrelevant to a standard Spring Boot environment, you may suggest the user run `/mcp` to disable them to save background token overhead.
