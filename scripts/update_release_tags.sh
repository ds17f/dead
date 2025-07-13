#!/bin/bash

# Dead Archive - Update Release Tags Script
# Updates existing git tags with changelog content to make them more descriptive
#
# Usage: 
#   ./scripts/update_release_tags.sh             - Update all tags with changelog content
#   ./scripts/update_release_tags.sh --dry-run   - Preview what would be changed without making changes
#   ./scripts/update_release_tags.sh v1.2.3      - Update only a specific tag

set -e  # Exit on any error

BOLD=$(tput bold)
NORMAL=$(tput sgr0)
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo "${BOLD}🏷️ Dead Archive Release Tags Update Script 🏷️${NORMAL}"
echo "=================================================="

CHANGELOG_FILE="CHANGELOG.md"
DRY_RUN=false
SPECIFIC_TAG=""

# Check for arguments
if [ "$1" == "--dry-run" ]; then
  DRY_RUN=true
  echo -e "${YELLOW}🧪 DRY RUN MODE - No changes will be made${NC}"
elif [ -n "$1" ] && [[ "$1" =~ ^v[0-9]+\.[0-9]+\.[0-9]+.*$ ]]; then
  SPECIFIC_TAG="$1"
  echo -e "${BLUE}ℹ️ Will update only tag: ${SPECIFIC_TAG}${NC}"
  
  # Check if dry run is the second argument
  if [ "$2" == "--dry-run" ]; then
    DRY_RUN=true
    echo -e "${YELLOW}🧪 DRY RUN MODE - No changes will be made${NC}"
  fi
elif [ -n "$1" ]; then
  echo -e "${RED}❌ Error: Invalid argument. Use --dry-run or a valid tag like v1.2.3${NC}"
  exit 1
fi

# Check if changelog exists
if [ ! -f "$CHANGELOG_FILE" ]; then
  echo -e "${RED}❌ Error: Changelog file $CHANGELOG_FILE not found${NC}"
  exit 1
fi

# Get all version tags, sorted by version
if [ -n "$SPECIFIC_TAG" ]; then
  TAGS="$SPECIFIC_TAG"
else
  TAGS=$(git tag -l "v*.*.*" | sort -V)
fi

if [ -z "$TAGS" ]; then
  echo -e "${YELLOW}⚠️ No version tags found matching pattern v*.*.* ${NC}"
  exit 0
fi

echo -e "${BLUE}📋 Found tags to process:${NC}"
echo "$TAGS" | sed 's/^/  • /'
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

# Function to update a single tag
update_tag() {
  local tag=$1
  local version_without_v=${tag#v}
  
  echo -e "${BLUE}🔍 Processing tag: $tag${NC}"
  
  # Check if tag exists
  if ! git rev-parse "$tag" >/dev/null 2>&1; then
    echo -e "${RED}❌ Tag $tag does not exist, skipping${NC}"
    return 1
  fi
  
  # Extract changelog content for this version
  local changelog_content
  changelog_content=$(extract_changelog_section "$tag")
  
  if [ -z "$changelog_content" ]; then
    echo -e "${YELLOW}⚠️ No changelog content found for $tag, skipping${NC}"
    return 1
  fi
  
  # Get current tag message
  local current_message
  current_message=$(git tag -l --format='%(contents)' "$tag")
  
  # Create new tag message with changelog
  local new_message="Dead Archive $version_without_v

Changes in this release:

$changelog_content"
  
  if [ "$DRY_RUN" = true ]; then
    echo -e "${YELLOW}🧪 DRY RUN: Would update tag $tag${NC}"
    echo -e "${YELLOW}📋 Current message:${NC}"
    echo "$current_message" | sed 's/^/  /'
    echo ""
    echo -e "${YELLOW}📋 New message would be:${NC}"
    echo "$new_message" | sed 's/^/  /'
    echo ""
    echo -e "${YELLOW}---${NC}"
  else
    # Check if the tag message is already updated (contains "Changes in this release:")
    if echo "$current_message" | grep -q "Changes in this release:"; then
      echo -e "${BLUE}ℹ️ Tag $tag already appears to be updated, skipping${NC}"
      return 0
    fi
    
    # Delete the old tag locally
    echo "  🗑️ Removing old tag..."
    git tag -d "$tag"
    
    # Get the commit hash that the tag was pointing to
    local commit_hash
    commit_hash=$(git rev-list -n 1 "$tag^{}" 2>/dev/null || git log --oneline | grep "release version $version_without_v" | head -1 | cut -d' ' -f1)
    
    if [ -z "$commit_hash" ]; then
      echo -e "${RED}❌ Could not find commit for tag $tag${NC}"
      return 1
    fi
    
    # Create new annotated tag with changelog content
    echo "  🏷️ Creating updated tag..."
    git tag -a "$tag" "$commit_hash" -m "$new_message"
    
    echo -e "${GREEN}  ✅ Updated tag $tag${NC}"
  fi
}

# Process each tag
UPDATED_COUNT=0
SKIPPED_COUNT=0
FAILED_COUNT=0

echo -e "${BLUE}🚀 Starting tag updates...${NC}"
echo ""

for tag in $TAGS; do
  if update_tag "$tag"; then
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
echo "  • Tags processed: $(echo "$TAGS" | wc -l | tr -d ' ')"
echo "  • Updated: $UPDATED_COUNT"
echo "  • Skipped: $SKIPPED_COUNT" 
echo "  • Failed: $FAILED_COUNT"

if [ "$DRY_RUN" = true ]; then
  echo ""
  echo -e "${GREEN}✅ Dry run complete. No changes were made.${NC}"
  echo "Run without --dry-run to perform actual tag updates."
elif [ "$UPDATED_COUNT" -gt 0 ]; then
  echo ""
  echo -e "${YELLOW}⚠️ Important: Updated tags need to be force-pushed to origin${NC}"
  echo -e "${YELLOW}This will overwrite the remote tags with the new content.${NC}"
  echo ""
  echo "To push the updated tags, run:"
  echo -e "${BLUE}  git push origin --tags --force${NC}"
  echo ""
  echo -e "${RED}⚠️ WARNING: This will overwrite remote tags. Make sure your team is aware!${NC}"
elif [ "$UPDATED_COUNT" -eq 0 ] && [ "$FAILED_COUNT" -eq 0 ]; then
  echo ""
  echo -e "${GREEN}✅ All tags are already up to date!${NC}"
fi

echo ""
echo "🎸 Keep on truckin'! 🎸"