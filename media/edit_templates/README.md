# Face Edit Templates

Drop your chroma-key templates here.

## Expected names:
- `male_mogged.mp4`
- `male_mogger.mp4`
- `woman_mogged.mp4`
- `woman_mogger.mp4`

## Supported extensions:
- `.mp4`
- `.mov`
- `.mkv`
- `.webm`

## How it works:
- The bot only makes edits for `face` ratings.
- `mogged` means "тебя могают".
- `mogger` means "ты могаешь".
- `neutral` does not create an edit.
- The user's face photo is stretched to the template resolution, then shown through the green-screen areas of the video.

## Folders:
- source face photos: `media/photos`
- rendered edits: `media/output`

## ffmpeg:
- The project tries `ffmpeg` from `PATH`.
- You can also pass a custom path with Java property `-Dbot.ffmpeg.path=...`
- Or via env var `FFMPEG_PATH`
- If the value points to a folder, the bot checks `<folder>\\ffmpeg.exe` and `<folder>\\bin\\ffmpeg.exe`
- Fallback path currently supported: `C:\Program Files\BlueStacks_nxt\ffmpeg.exe`
