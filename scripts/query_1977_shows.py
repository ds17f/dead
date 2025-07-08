#!/usr/bin/env python3
"""
Quick script to find and generate ratings for 1977 Grateful Dead shows.

This will help us get some real ratings data to test the UI with.
"""

import json
import requests
import time
from typing import List, Dict, Optional

def get_1977_grateful_dead_shows(max_shows: int = 20) -> List[Dict]:
    """Get Grateful Dead shows from 1977."""
    print("🔍 Searching for 1977 Grateful Dead shows...")
    
    try:
        search_url = "https://archive.org/advancedsearch.php"
        params = {
            'q': 'collection:GratefulDead AND date:[1977-01-01 TO 1977-12-31] AND mediatype:etree',
            'fl': 'identifier,date,title,venue,description,avg_rating,num_reviews',
            'sort[]': 'date asc',
            'rows': max_shows,
            'output': 'json'
        }
        
        response = requests.get(search_url, params=params, timeout=60)
        response.raise_for_status()
        
        search_results = response.json()
        shows = []
        
        for doc in search_results.get('response', {}).get('docs', []):
            identifier = doc.get('identifier')
            date = doc.get('date')
            venue = doc.get('venue', 'Unknown Venue')
            avg_rating = doc.get('avg_rating')
            num_reviews = doc.get('num_reviews', 0)
            
            if identifier and date:
                shows.append({
                    'identifier': identifier,
                    'date': date[:10] if len(date) > 10 else date,  # Normalize to YYYY-MM-DD
                    'venue': venue,
                    'avg_rating': float(avg_rating) if avg_rating else None,
                    'num_reviews': int(num_reviews) if num_reviews else 0
                })
        
        print(f"✅ Found {len(shows)} shows from 1977")
        return shows
        
    except Exception as e:
        print(f"❌ Error searching for shows: {e}")
        return []

def create_sample_ratings_json(shows: List[Dict]) -> Dict:
    """Create a sample ratings JSON with 1977 shows."""
    recording_ratings = {}
    show_ratings = {}
    
    for show in shows:
        identifier = show['identifier']
        date = show['date']
        venue = show['venue']
        avg_rating = show.get('avg_rating')
        num_reviews = show.get('num_reviews', 0)
        
        # Only include if there's actual rating data
        if avg_rating and avg_rating > 0 and num_reviews > 0:
            # Recording rating
            recording_ratings[identifier] = {
                'rating': round(avg_rating, 1),
                'review_count': num_reviews,
                'source_type': 'SBD' if 'sbd' in identifier.lower() else 'AUD',
                'confidence': min(num_reviews / 10.0, 1.0)  # Higher confidence with more reviews
            }
            
            # Show rating (generate show key)
            venue_clean = venue.replace(' ', '_').replace(',', '').replace('&', 'and').replace("'", "")
            show_key = f"{date}_{venue_clean}"
            
            show_ratings[show_key] = {
                'date': date,
                'venue': venue,
                'rating': round(avg_rating, 1),
                'confidence': min(num_reviews / 10.0, 1.0),
                'best_recording': identifier,
                'recording_count': 1
            }
    
    return {
        'metadata': {
            'generated_at': '2024-01-01T12:00:00Z',
            'version': '1.0.0',
            'source': '1977 Grateful Dead Archive.org data',
            'total_recordings': len(recording_ratings),
            'total_shows': len(show_ratings)
        },
        'recording_ratings': recording_ratings,
        'show_ratings': show_ratings
    }

def main():
    """Main execution."""
    print("🎸 Generating sample ratings from 1977 Grateful Dead shows...\n")
    
    # Get 1977 shows
    shows = get_1977_grateful_dead_shows(max_shows=50)
    
    if not shows:
        print("❌ No shows found")
        return
    
    # Show summary
    rated_shows = [s for s in shows if s.get('avg_rating') and s['avg_rating'] > 0]
    print(f"📊 Summary:")
    print(f"   Total shows found: {len(shows)}")
    print(f"   Shows with ratings: {len(rated_shows)}")
    
    if rated_shows:
        avg_rating = sum(s['avg_rating'] for s in rated_shows) / len(rated_shows)
        print(f"   Average rating: {avg_rating:.1f}")
        
        # Show top rated shows
        top_shows = sorted(rated_shows, key=lambda x: x['avg_rating'], reverse=True)[:5]
        print(f"\n🌟 Top 5 rated shows from 1977:")
        for i, show in enumerate(top_shows, 1):
            print(f"   {i}. {show['date']} - {show['venue']} ({show['avg_rating']:.1f}⭐, {show['num_reviews']} reviews)")
    
    # Generate ratings JSON
    ratings_data = create_sample_ratings_json(shows)
    
    # Save to file
    output_path = 'app/src/main/assets/sample_ratings.json'
    try:
        with open(output_path, 'w') as f:
            json.dump(ratings_data, f, indent=2, sort_keys=True)
        
        print(f"\n✅ Sample ratings saved to {output_path}")
        print(f"   Recordings with ratings: {len(ratings_data['recording_ratings'])}")
        print(f"   Shows with ratings: {len(ratings_data['show_ratings'])}")
        
    except Exception as e:
        print(f"❌ Error saving ratings: {e}")

if __name__ == '__main__':
    main()