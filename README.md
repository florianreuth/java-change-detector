# java-change-detector

Simple tool for monitoring online resources and detecting changes.

## Downloads

You can download the latest jar file
from [my build server](https://build.florianreuth.de/job/java-change-detector), [GitHub Actions](https://github.com/florianreuth/java-change-detector/actions)
or use the [releases tab](https://github.com/florianreuth/java-change-detector/releases).

## Usage

1. Run the JAR file:
   ```bash
   java -jar java-change-detector.jar
   ```

2. Edit the generated `config.json` file to configure your monitors

3. Run again to start monitoring

## Configuration

Edit `config.json`:

```json
{
    "monitors": [
        {
            "name": "minecraft-versions",
            "url": "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json",
            "interval": "1d",
            "path": "$.latest.release",
            "execution": [
                "echo 'New version: ${NEW_VALUE}'"
            ]
        }
    ]
}
```

**Fields:**

- `name` - Monitor identifier
- `url` - URL to check
- `interval` - Check interval (e.g., `30s`, `5m`, `1h`, `1d`). Supported units:
  - `s` - seconds
  - `m` - minutes
  - `h` - hours
  - `d` - days
  
  Minimum interval: 10 seconds
- `path` - JsonPath to extract value (optional, e.g., `$.latest.release`)
- `execution` - Commands to run on change (optional, can use `${OLD_VALUE}` and `${NEW_VALUE}`)

**Example: Monitor GitHub releases**

```json
{
    "name": "my-app",
    "url": "https://api.github.com/repos/owner/repo/releases/latest",
    "interval": "10m",
    "path": "$.tag_name"
}
```

Logs are written to `logs/latest.log`

## Contact

If you encounter any issues, please report them on the
[issue tracker](https://github.com/florianreuth/java-change-detector/issues).  
If you just want to talk or need help with java-change-detector feel free to join my
[Discord](http://florianreuth.de/discord).
