# Contributing to Linux-and-Android

Thank you for your interest in contributing to Linux-and-Android! This document provides guidelines for contributors.

## 🤔 What You Can Contribute

Any of the following would be greatly appreciated:

### ✅ Code Contributions
- **New Features**: Add support for additional Linux distributions, desktop environments, or tools
- **Bug Fixes**: Report and fix issues in the application
- **Performance Improvements**: Optimize setup speed, memory usage, or application performance
- **Refactoring**: Improve code quality, maintainability, and readability

### ✅ Documentation
- Translate documentation to other languages
- Add code comments and docstrings
- Create setup guides and tutorials
- Improve the README with helpful examples
- Document known issues and limitations

### ✅ Testing
- Test on various Android versions and devices
- Report bugs and provide detailed reproduction steps
- Write and maintain test cases

### ✅ Feedback
- Report bugs (please include device info and clear steps to reproduce)
- Suggest new features
- Share your use cases and stories

## 📋 Getting Started

### 1. Fork the Repository
Click the "Fork" button at the top right of the repository page.

### 2. Clone Your Fork
```bash
git clone https://github.com/SHARVANKUMARSINGH/linux-andriod.git
cd linux-andriod
```

### 3. Create a Branch
```bash
git checkout -b feature/your-feature-name
# or
git checkout -b bugfix/your-bug-description
```

### 4. Make Your Changes
- Follow the existing code style (Kotlin conventions)
- Add necessary tests for new features
- Update documentation if needed
- Keep changes focused and focused on a single issue

### 5. Test Your Changes
```bash
./gradlew clean build test
```

### 6. Commit Your Changes
```bash
git add .
git commit -m "feat: Add some feature
                   sunmit by: John Doe"

# Use conventional commits format
# feat:     New feature
# fix:      Bug fix
# docs:     Documentation only changes
# style:    Changes that don't affect code meaning
# refactor: Code refactoring
# perf:     Performance improvements
# test:     Adding tests
# chore:    Other changes
```

### 7. Push and Create a Pull Request
```bash
git push origin feature/your-feature-name
```

Then go to the original repository and create a Pull Request from your fork.

## 🎨 Code Style Guidelines

### Kotlin Style
- Follow [Android Kotlin Style Guide](https://developer.android.com/kotlin/style-guide)
- Use KTX extensions when appropriate
- Follow snake_case for variable and method names
- Use camelCase for class and object names (PascalCase)

### Code Organization
- Keep related classes and files together
- Use meaningful package names
- Provide clear and descriptive function names
- Add Javadoc/KDoc comments for public APIs

### Testing
- Write tests for new features
- Use Kotlin's `suspend` functions for async operations
- Test edge cases and error conditions
- Minimize test dependencies

## 🐛 Reporting Bugs

When reporting a bug, please include:

1. **Clear Description**: What exactly should work vs. what's not working
2. **Steps to Reproduce**: Exact steps to cause the bug
3. **Environment**:
   - Android version and device model
   - App version/build number
   - Termux version if applicable
4. **Log Files**: Output logcat for the application
   ```bash
   adb logcat -d > your-log-file.txt
   ```
5. **Screenshots/Video**: Visual demonstration of the issue

## ✨ Requesting Features

When requesting features, please include:

1. **Feature Description**: What would you like to have?
2. **Use Case**: Why would this be useful?
3. **Alternative Solutions**: How would you currently accomplish this?
4. **Proposed Implementation**: If you have an idea how it could be implemented, describe it

## 💬 Code Review Process

1. Your PR will be reviewed by maintainers
2. Comments and feedback may be added
3. Adjustments may be requested
4. Once approved, the PR will be merged

## 🤝 Before You Contribute

- Read this entire contributing guide
- Check for existing issues similar to yours
- Search the discussions for similar topics
- Follow the repository's code of conduct
- Be respectful and constructive

## 📧 Contact

For questions or discussions:
- Open a GitHub Issue
- Join GitHub Discussions
- Check the issues and discussions pages

## 🌟 Special Recognition

Contributors will be acknowledged in:
- The README.md contributors section
- Release notes
- The project's credits

## 📜 License

By contributing, you agree that your contributions will be licensed under the MIT License.

---

Created with ❤️ by the open-source community!

[⬆ Back to Top](#contributing-to-linux-and-android)
