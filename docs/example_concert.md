# Example Grateful Dead Audience Recording

## Concert Information

| Attribute | Value |
|-----------|-------|
| Date | 1973-06-10 |
| Location | RFK Stadium, Washington, DC |
| Identifier | gd73-06-10.aud.weiner.gdadt.26267.sbeok.shnf |
| Source | Audience recording |
| Taper | Jim Weiner |
| Lineage | Master cassette > DAT > CD > SHN |

## Access URLs

### Metadata URL
```
https://archive.org/metadata/gd73-06-10.aud.weiner.gdadt.26267.sbeok.shnf
```

### Direct File Access Examples
```
https://archive.org/download/gd73-06-10.aud.weiner.gdadt.26267.sbeok.shnf/gd73-06-10d1t01.shn
https://archive.org/download/gd73-06-10.aud.weiner.gdadt.26267.sbeok.shnf/gd73-06-10d1t02.shn
https://archive.org/download/gd73-06-10.aud.weiner.gdadt.26267.sbeok.shnf/gd73-06-10d2t01.shn
```

### Embed Player URL
```
https://archive.org/embed/gd73-06-10.aud.weiner.gdadt.26267.sbeok.shnf
```

## Setlist

### Set 1
1. Morning Dew
2. Mexicali Blues
3. Brown-Eyed Women
4. Beat It On Down The Line
5. Tennessee Jed
6. Jack Straw
7. Row Jimmy
8. El Paso
9. Playing In The Band

### Set 2
1. Eyes Of The World
2. Stella Blue
3. Big River
4. Dark Star
5. He's Gone
6. Sugar Magnolia

### Encore
1. Johnny B. Goode

## Special Notes

This is a notable audience recording from the legendary RFK Stadium show in 1973. The sound quality is typical of audience recordings from this era - some ambient noise but good spatial feel and crowd energy. The weather was hot and humid, which can be heard in audience reactions between songs.

## Sample Code for Accessing

```java
// Example using the audience recording identifier
String identifier = "gd73-06-10.aud.weiner.gdadt.26267.sbeok.shnf";
String song = "gd73-06-10d1t01.shn"; // Morning Dew

// Construct streaming URL
String streamUrl = "https://archive.org/download/" + identifier + "/" + song;

// Set up ExoPlayer for playback
MediaItem mediaItem = MediaItem.fromUri(streamUrl);
exoPlayer.setMediaItem(mediaItem);
exoPlayer.prepare();
exoPlayer.play();
```

## Differences from Soundboard Recordings

Audience recordings like this have distinct characteristics compared to soundboard recordings:

1. **Ambient Sound**: Includes crowd noise, room acoustics, and applause
2. **Balanced Mix**: Often has a more natural blend of instruments than direct soundboard feed
3. **Authenticity**: Captures the experience of being in the audience
4. **Variable Quality**: Sound quality can vary depending on taper equipment and position

When implementing playback, you may want to adjust equalization settings to compensate for the characteristics of audience recordings, which often have different frequency profiles than soundboard recordings.