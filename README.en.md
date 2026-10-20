# Better Forbidden and Arcanus

[简体中文](README.md) · English

Better Forbidden and Arcanus expands the original mod with five tiers of equipment upgrades, wearable relics, and new abilities for familiar weapons and armor. Combine prisms on the Runic Battleaxe, improve essence production and Clibano processing, and discover expanded crafting options. An in-game guide in English and Simplified Chinese helps you explore it all.

## Highlights

- Upgrade equipment through the five tiers of the Hephaestus Forge. Higher-tier forges can also perform lower-tier equipment upgrades.
- Equip relics in a dedicated Curios slot and combine their abilities.
- Stack four different prisms on the Runic Battleaxe, with returning throws and a special completed form.
- Discover expanded Draco Arcanus and Tyr equipment abilities, new rituals, and additional recipes.
- Improve jars, essence transfer, obelisks, and soul-powered Clibano processing.
- Craft **Hephaestus' Journal** with an Arcane Crystal and a book in a shapeless recipe. Its English and Simplified Chinese editions follow your game language.

## Installation

Install the addon and its required dependencies on **both client and server**:

| Component | Version used for development |
| --- | --- |
| Minecraft | 1.21.1 |
| NeoForge | 21.1.251 |
| Forbidden and Arcanus | 2.6.1 (required exact version) |
| Valhelsia Core | 1.1.5 for NeoForge 1.21.1 |
| Curios API | 9.5.1+1.21.1 for NeoForge |
| Patchouli | 1.21.1-93-NEOFORGE |

Download `better_forbidden_arcanus-0.14.0.jar` from this repository's Releases page and place it in `mods`. Keep only one version of this addon installed. Dependencies are separate downloads and are not bundled. JEI is optional for gameplay.

Disable or regenerate older exported guide resource packs if they override the bundled book.

## Build from source

Use **JDK 21**. Place the five dependency JARs listed above in `libs/`. To compile the development checks, also add **JEI 19.57.0.448 for NeoForge 1.21.1** to `libs/`. These JARs are ignored by Git and are not included in the addon.

On Windows, you can copy the required dependencies from an existing Minecraft instance:

```powershell
.\scripts\setup-dev.ps1 -InstanceDirectory 'C:\path\to\minecraft-instance'
# Add the JEI development JAR to libs/ separately.
$env:BFA_JDK_HOME = 'C:\path\to\jdk-21'
.\scripts\dev.ps1 build
```

Alternatively, configure `JAVA_HOME` for JDK 21 and use `gradlew.bat build` on Windows or `bash ./gradlew build` on Linux/macOS. Gradle downloads its own build tooling and Minecraft/NeoForge development files. The addon JAR is written to `build/libs/`.

The local visual guide editor requires Python 3. See [the editor guide](docs/GUIDE_EDITOR.md). It edits Simplified Chinese by default; `--lang en_us` selects English. Save changes and rebuild the addon to distribute them.

## Credits and license

This is an unofficial addon for [Forbidden and Arcanus](https://github.com/stal111/Forbidden-Arcanus), by stal111 and its contributors. Selected historical content and artwork originate from that project; see [the source and adaptation notes](docs/LEGACY_CONTENT.md). Patchouli provides the in-game guide framework.

Original addon code currently uses **All Rights Reserved**. This declaration does not claim ownership of third-party code, artwork, or build tooling, or change their terms. See [LICENSE](LICENSE), [TEMPLATE_LICENSE.txt](TEMPLATE_LICENSE.txt), and [third-party notices](THIRD_PARTY_NOTICES.md).

See [CHANGELOG.md](CHANGELOG.md) for version history.
