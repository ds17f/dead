#!/bin/bash

# Dead Archive - Update GitHub Releases Script
# Updates existing GitHub releases with changelog content to make them more descriptive
#
# Usage: 
#   ./scripts/update_release_descriptions.sh             - Update all GitHub release descriptions with changelog content
#   ./scripts/update_release_descriptions.sh --dry-run   - Preview what would be changed without making changes
#   ./scripts/update_release_descriptions.sh v1.2.3      - Update only a specific release

set -e  # Exit on any error

BOLD=$(tput bold)
NORMAL=$(tput sgr0)
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo "${BOLD}🚀 Dead Archive GitHub Releases Update Script 🚀${NORMAL}"
echo "===================================================="

CHANGELOG_FILE="CHANGELOG.md"
DRY_RUN=false
SPECIFIC_TAG=""

# Check for arguments
if [ "$1" == "--dry-run" ]; then
  DRY_RUN=true
  echo -e "${YELLOW}🧪 DRY RUN MODE - No changes will be made${NC}"
elif [ -n "$1" ] && [[ "$1" =~ ^v[0-9]+\.[0-9]+\.[0-9]+.*$ ]]; then
  SPECIFIC_TAG="$1"
  echo -e "${BLUE}ℹ️ Will update only release: ${SPECIFIC_TAG}${NC}"
  
  # Check if dry run is the second argument
  if [ "$2" == "--dry-run" ]; then
    DRY_RUN=true
    echo -e "${YELLOW}🧪 DRY RUN MODE - No changes will be made${NC}"
  fi
elif [ -n "$1" ]; then
  echo -e "${RED}❌ Error: Invalid argument. Use --dry-run or a valid release tag like v1.2.3${NC}"
  exit 1
fi

# Check if gh CLI is available
if ! command -v gh &> /dev/null; then
  echo -e "${RED}❌ Error: GitHub CLI (gh) is required but not installed${NC}"
  echo -e "${BLUE}ℹ️ Install it with: brew install gh (macOS) or apt install gh (Ubuntu)${NC}"
  echo -e "${BLUE}ℹ️ Then authenticate with: gh auth login${NC}"
  exit 1
fi

# Check if authenticated with GitHub
if ! gh auth status &> /dev/null; then
  echo -e "${RED}❌ Error: Not authenticated with GitHub${NC}"
  echo -e "${BLUE}ℹ️ Run: gh auth login${NC}"
  exit 1
fi

# Check if changelog exists
if [ ! -f "$CHANGELOG_FILE" ]; then
  echo -e "${RED}❌ Error: Changelog file $CHANGELOG_FILE not found${NC}"
  exit 1
fi

# Get all GitHub releases, sorted by version
if [ -n "$SPECIFIC_TAG" ]; then
  RELEASES="$SPECIFIC_TAG"
else
  RELEASES=$(gh release list --limit 100 | grep -E "v[0-9]+\.[0-9]+\.[0-9]+" | awk '{print $1}' | sort -V)
fi

if [ -z "$RELEASES" ]; then
  echo -e "${YELLOW}⚠️ No GitHub releases found matching pattern v*.*.* ${NC}"
  exit 0
fi

echo -e "${BLUE}📋 Found releases to process:${NC}"
echo "$RELEASES" | sed 's/^/  • /'
echo ""

# Function to extract changelog section for a specific version
extract_changelog_section() {
  local version=$1
  local version_without_v=${version#v}  # Remove 'v' prefix
  
  # Extract the section for this version from the changelog
  awk "
    /## \[${version_without_v}\]/ { 
      flag=1; 
      print; 
      next 
    } 
    /## \[/ && flag { 
      flag=0 
    } 
    flag && !/^$/ { 
      print 
    }
  " "$CHANGELOG_FILE"
}

# Function to update a single GitHub release
update_release() {
  local release_tag=$1
  local version_without_v=${release_tag#v}
  
  echo -e "${BLUE}🔍 Processing release: $release_tag${NC}"
  
  # Check if release exists
  if ! gh release view "$release_tag" >/dev/null 2>&1; then
    echo -e "${RED}❌ Release $release_tag does not exist, skipping${NC}"
    return 1
  fi
  
  # Extract changelog content for this version
  local changelog_content
  changelog_content=$(extract_changelog_section "$release_tag")
  
  if [ -z "$changelog_content" ]; then
    echo -e "${YELLOW}⚠️ No changelog content found for $release_tag, skipping${NC}"
    return 1
  fi
  
  # Get current release description
  local current_description
  current_description=$(gh release view "$release_tag" --json body --jq '.body')
  
  # Create new release description with changelog
  local new_description="$changelog_content"
  
  if [ "$DRY_RUN" = true ]; then
    echo -e "${YELLOW}🧪 DRY RUN: Would update release $release_tag${NC}"
    echo -e "${YELLOW}📋 Current description:${NC}"
    echo "$current_description" | sed 's/^/  /'
    echo ""
    echo -e "${YELLOW}📋 New description would be:${NC}"
    echo "$new_description" | sed 's/^/  /'
    echo ""
    echo -e "${YELLOW}---${NC}"
  else
    # Check if the release description is already updated (contains changelog sections)
    if echo "$current_description" | grep -q "### New Features\|### Bug Fixes\|### "; then
      echo -e "${BLUE}ℹ️ Release $release_tag already appears to be updated, skipping${NC}"
      return 0
    fi
    
    # Update the GitHub release with new description
    echo "  📝 Updating release description..."
    if gh release edit "$release_tag" --notes "$new_description"; then
      echo -e "${GREEN}  ✅ Updated release $release_tag${NC}"
    else
      echo -e "${RED}  ❌ Failed to update release $release_tag${NC}"
      return 1
    fi
  fi
}

# Process each release
UPDATED_COUNT=0
SKIPPED_COUNT=0
FAILED_COUNT=0

echo -e "${BLUE}🚀 Starting GitHub release updates...${NC}"
echo ""

for release in $RELEASES; do
  if update_release "$release"; then
    UPDATED_COUNT=$((UPDATED_COUNT + 1))
  else
    if [ $? -eq 1 ]; then
      FAILED_COUNT=$((FAILED_COUNT + 1))
    else
      SKIPPED_COUNT=$((SKIPPED_COUNT + 1))
    fi
  fi
done

echo ""
echo -e "${BOLD}📊 Summary:${NORMAL}"
echo "  • Releases processed: $(echo "$RELEASES" | wc -l | tr -d ' ')"
echo "  • Updated: $UPDATED_COUNT"
echo "  • Skipped: $SKIPPED_COUNT" 
echo "  • Failed: $FAILED_COUNT"

if [ "$DRY_RUN" = true ]; then
  echo ""
  echo -e "${GREEN}✅ Dry run complete. No changes were made.${NC}"
  echo "Run without --dry-run to perform actual GitHub release updates."
elif [ "$UPDATED_COUNT" -gt 0 ]; then
  echo ""
  echo -e "${GREEN}✅ GitHub releases updated successfully!${NC}"
  echo -e "${BLUE}ℹ️ The changes are immediately visible on GitHub releases page.${NC}"
elif [ "$UPDATED_COUNT" -eq 0 ] && [ "$FAILED_COUNT" -eq 0 ]; then
  echo ""
  echo -e "${GREEN}✅ All GitHub releases are already up to date!${NC}"
fi

echo ""
echo "🎸 Keep on truckin'! 🎸"