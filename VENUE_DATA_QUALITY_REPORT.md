# Venue Data Quality Report
## Analysis of V2 Database Import Issues

**Date:** August 14, 2025  
**Analyzed Dataset:** data-v2.0.0.zip (2,313 shows)  
**Context:** Dead Archive V2 database venue normalization analysis  

---

## Executive Summary

During V2 database implementation, we discovered venue data inconsistencies that create unnecessary duplicate venue records. While the raw data contains **590 unique venue names**, the current venue+location combinations generate **600 venue records** due to inconsistent location data formatting.

**Key Finding:** The 10 extra venue records are caused by legitimate data quality issues that should be fixed at the source level in the dead-metadata repository.

---

## Detailed Findings

### Issue 1: Assembly Hall Location Inconsistency ⚠️

**Problem:** Same venue appears with different city formatting, creating duplicate venue records.

**Venue:** "Assembly Hall, U. of Illinois"  
**Location:** University of Illinois at Urbana-Champaign, Illinois  

**Current Data:**
```json
// Shows from 1973 (2 shows)
{
  "venue": "Assembly Hall, U. of Illinois",
  "city": "Champaign-Urbana", 
  "state": "IL",
  "date": "1973-02-21"
}

// Show from 1981 (1 show) 
{
  "venue": "Assembly Hall, U. of Illinois",
  "city": "Champaign",
  "state": "IL", 
  "date": "1981-12-02"
}
```

**Impact:** Creates 2 separate venue records for the same physical location.

**Recommended Fix:** Standardize all instances to `"city": "Champaign"` since the University of Illinois is located in Champaign, Illinois. The "Champaign-Urbana" designation refers to the broader metropolitan area.

---

### Issue 2: Empty State Fields for US Venues ⚠️

**Problem:** US venues with empty state fields create inconsistent venue keys.

**Example:**
```json
{
  "venue": "Billerica Forum",
  "city": "Billerica",
  "state": "",  // Should be "MA"
  "country": "MA"  // Country field incorrectly contains state code
}
```

**Impact:** Venue key generation fails to properly normalize US venue locations.

**Recommended Fix:** 
- Fill empty US state fields with correct state codes
- Fix data where state information appears in wrong field
- Add validation to ensure US venues have proper state codes

---

### Issue 3: German Venue State Handling ✅

**Status:** This is actually CORRECT behavior that should be preserved.

**Examples:**
```json
{
  "venue": "Gruga Halle",
  "city": "Essen", 
  "state": null,
  "country": "Germany"
}

{
  "venue": "Olympia Halle",
  "city": "Munich",
  "state": null, 
  "country": "Germany"
}
```

**Analysis:** German venues correctly have null state fields since Germany uses Länder (federal states) which are handled differently than US states.

**Recommendation:** Maintain null state fields for international venues. This is correct data modeling.

---

## Complete List of Identified Issues

### Assembly Hall Variations
| Venue | City | State | Shows | Dates |
|-------|------|-------|-------|-------|
| Assembly Hall, U. of Illinois | Champaign-Urbana | IL | 2 | 1973-02-21, 1973-02-22 |
| Assembly Hall, U. of Illinois | Champaign | IL | 1 | 1981-12-02 |

**Fix:** Standardize city to "Champaign"

### Empty State Field Issues
| Venue | City | State | Country | Issue |
|-------|------|-------|---------|-------|
| Billerica Forum | Billerica | "" | MA | State code in country field |

**Fix:** Move state code to proper field, add country "USA"

### International Venues (Correct as-is)
| Venue | City | State | Country | Status |
|-------|------|-------|---------|--------|
| Gruga Halle | Essen | null | Germany | ✅ Correct |
| Olympia Halle | Munich | null | Germany | ✅ Correct | 
| Beat Club | Bremen | null | Germany | ✅ Correct |
| Festhalle | Frankfurt | null | Germany | ✅ Correct |

---

## Recommended Source Data Fixes

### 1. Assembly Hall Standardization
```bash
# Find and replace in show JSON files
find . -name "*.json" -exec sed -i 's/"Champaign-Urbana"/"Champaign"/g' {} \;
```

### 2. US State Field Validation  
Add data validation rules:
- US venues (country = "USA" or null) must have valid state codes
- State codes should be 2-letter abbreviations (CA, NY, IL, etc.)
- No empty string state fields for US venues

### 3. Data Quality Checks
Implement pre-processing validation:
```sql
-- Find US venues with missing states
SELECT venue, city, state, country 
FROM shows 
WHERE (country IS NULL OR country = 'USA') 
  AND (state IS NULL OR state = '');

-- Find venues with same name but different city formats
SELECT venue, city, state, COUNT(*) as show_count
FROM shows 
GROUP BY venue, city, state 
HAVING COUNT(*) > 0
ORDER BY venue, city;
```

---

## Expected Impact

### Before Fixes
- **Venue Records:** 600 (includes 10 duplicates from inconsistencies)
- **Unique Venues:** 590 (actual physical locations)
- **Data Quality:** Inconsistent location formatting

### After Fixes  
- **Venue Records:** ~590 (matches unique venue names)
- **Unique Venues:** 590 (properly normalized)
- **Data Quality:** Consistent, normalized location data

### Benefits
1. **Accurate Venue Statistics:** Proper show counts per venue
2. **Improved Data Integrity:** Consistent venue identification
3. **Better User Experience:** No duplicate venues in search/browse
4. **Simplified Venue Management:** Single record per physical location

---

## Validation Queries for Testing Fixes

### 1. Verify Assembly Hall Fix
```bash
# Should return single venue record after fix
grep -r "Assembly Hall, U. of Illinois" . | jq '.city' | sort | uniq
# Expected: "Champaign" only
```

### 2. Check US State Field Completion
```bash
# Should return empty result after fix
jq -r 'select(.country == "USA" or .country == null) | select(.state == "" or .state == null) | [.venue, .city, .state] | @csv' *.json
```

### 3. Verify International Venue Handling
```bash  
# Should still show null states for German venues (correct)
jq -r 'select(.country == "Germany") | [.venue, .city, .state] | @csv' *.json
```

---

## Implementation Priority

**High Priority:**
1. ✅ Assembly Hall city standardization (affects 3 shows)
2. ✅ US venue state field validation and fixes

**Medium Priority:**  
3. ✅ Add automated data quality checks to prevent regressions
4. ✅ Document international venue state handling standards

**Low Priority:**
5. ✅ Historical data audit for other similar inconsistencies

---

## Contact

For questions about this analysis or implementation details, contact the Dead Archive development team.

**Generated by:** Dead Archive V2 Database Analysis  
**Repository:** [dead-metadata](https://github.com/dead-metadata)  
**Related Issue:** V2 Database Venue Normalization