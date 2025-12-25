# Git Setup for Personal Account

The repository has been initialized, but you'll need to configure your personal GitHub account details.

## Step 1: Configure Your Personal Git Identity (Local to this repo only)

Run these commands in the project directory to set your personal account for this repo only:

```bash
cd /Users/lsn-snehith/react-native-floating-app-widget

# Set your personal name and email (local to this repo)
git config user.name "Your Name"
git config user.email "your-personal-email@example.com"

# Fix the initial commit author
git commit --amend --reset-author --no-edit
```

## Step 2: Create GitHub Repository

1. Go to your personal GitHub account
2. Create a new repository (e.g., `react-native-floating-app-widget`)
3. **Don't** initialize with README (we already have files)

## Step 3: Connect to GitHub

```bash
# Add your personal GitHub repo as remote
git remote add origin https://github.com/YOUR-USERNAME/react-native-floating-app-widget.git

# Or use SSH (recommended)
git remote add origin git@github.com:YOUR-USERNAME/react-native-floating-app-widget.git

# Push to GitHub
git branch -M main
git push -u origin main
```

## Step 4: Update package.json

Update the repository URLs in `package.json`:

```json
{
  "repository": "https://github.com/YOUR-USERNAME/react-native-floating-app-widget",
  "bugs": {
    "url": "https://github.com/YOUR-USERNAME/react-native-floating-app-widget/issues"
  },
  "homepage": "https://github.com/YOUR-USERNAME/react-native-floating-app-widget#readme",
  "author": "Your Name <your-personal-email@example.com>"
}
```

Then commit the changes:

```bash
git add package.json
git commit -m "Update repository URLs and author info"
git push
```

## Current Status

✅ Git repository initialized
✅ Initial commit created (32 files, 4082 lines)
⏳ Waiting for you to configure personal account details
⏳ Waiting for GitHub remote setup

## Verification

After setup, verify with:

```bash
# Check your local config
git config user.name
git config user.email

# Check remote
git remote -v

# Check commit author
git log --format='%an <%ae>' -1
```

## Important Notes

- The git config commands above will **only affect this repository**
- Your corporate profile (Snehithm17) will remain unchanged for other repos
- If you want to use SSH, make sure you've added your SSH key to your personal GitHub account
- After pushing, you can make the repository public or keep it private

## Alternative: Use Different Git Credential Helper

If you want to ensure you're always prompted for credentials for this repo:

```bash
# Disable credential helper for this repo
git config credential.helper ""
```

This will prompt you for username/password (or token) each time you push, ensuring you use your personal account.
