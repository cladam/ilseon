---
name: code-analyzer
description: Code quality analysis skill for ilseon, covering smells, maintainability, and refactoring guidance.
version: 1.1.0
author: Claes Adamsson
tags:
  - analysis
  - quality
  - refactoring
  - kotlin
  - android
  - compose
---

# Code Analyzer Skill

## Overview

Use this skill to provide focused code quality reviews for the ilseon codebase. The goal is to surface maintainability risks, code smells, and refactoring options with clear, actionable guidance.

## When to Use

- Reviewing a feature, module, or pull request for code quality.
- Assessing technical debt or refactoring scope.
- Identifying maintainability, readability, or performance risks.

## When Not to Use

- Security reviews (use the security skill).
- Architectural discovery or ADRs (use the architect skill).
- Test design or acceptance coverage planning (use the atdd-developer skill).

## Instructions

### Analysis Focus

- Identify code smells and anti-patterns.
- Evaluate complexity, cohesion, and coupling.
- Check consistency with project standards and Kotlin/Android/Jetpack Compose conventions.
- Suggest pragmatic, low-risk refactors.
- Cross-check Kotlin-specific findings and refactor guidance with the kotlin-developer skill.

### Consultation

- When proposing Kotlin refactors, consult the kotlin-developer skill to confirm idiomatic patterns, coroutine usage, and persistence guidance.
- If changes affect behaviour, recommend an ATDD update and note any test impact.

### Analysis Criteria

- **Readability**: clear naming, simple flows, meaningful comments.
- **Maintainability**: small functions, focused classes, low cyclomatic complexity.
- **Performance**: no obvious bottlenecks or wasteful work.
- **Security**: avoid obvious vulnerabilities and unsafe input handling.
- **Best Practices**: pragmatic use of patterns, DRY/KISS, predictable error handling.

### Code Smell Signals

- Long methods (about 50+ lines).
- Large classes (about 500+ lines).
- Duplicate or dead code.
- Complex conditionals or nested branching.
- Feature envy, inappropriate intimacy, or god objects.

### Code Smell Catalogue (Reference)

Use this catalogue as a reference when naming smells and explaining impact. Keep it concise in reports and only expand when a smell is confirmed.

- **Bloaters**: Large Class, Long Method, Long Parameter List, Data Clump.
- **Change Preventers**: Shotgun Surgery, Divergent Change.
- **Couplers**: Feature Envy, Message Chain, Parallel Inheritance Hierarchies.
- **Dispensables**: Dead Code, Duplicate Code, Lazy Element.
- **Data Issues**: Primitive Obsession, Global Data, Mutable Data.
- **Naming/Clarity**: Uncommunicative Name, Inconsistent Names, Fallacious Comment.

### Sources

- Martin Fowler, *Refactoring* (1999/2018)
- William C. Wake, *Refactoring Workbook* (2004)
- Robert C. Martin, *Clean Code* (2008)
- Marcel Jerzyk, *Code Smells: A Comprehensive Online Catalog and Taxonomy* (2022)

## Output Expectations

- Provide findings ordered by severity with file and line references.
- Offer concrete refactoring suggestions with minimal disruption.
- Call out positive patterns to reinforce good practice.
- Use a concise report format when the review is extensive.
- When Kotlin refactors are proposed, note alignment with kotlin-developer guidance or call out any open questions.

### Devlog

- Save every review in `.github/devlog/YYYY-MM-DD-review.md`.
- Record any refactoring carried out based on review findings in `.github/devlog/YYYY-MM-DD-activity.md`.
- Use this format for activity entries: `[AGENT_NAME]` -> `[ACTION_TAKEN]` -> `[RESULT/LINK]`.

### Preferred Report Shape

```markdown
## Code Quality Review

### Findings
1. [Severity] Issue summary
   - File: path/to/file.kt:line
   - Why it matters: ...
   - Suggested change: ...

### Positives
- ...

### Risks / Follow-ups
- ...
```

## Notes

- Keep the tone clear, inclusive, and action-oriented.
- Prefer evidence-based observations over speculation.
- When unsure, propose a small experiment to validate the issue.
- If a refactor changes behaviour, recommend an acceptance test update or new test first.
