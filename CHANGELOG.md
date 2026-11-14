# SPD (Shattered Pixel Dungeon-inspired)

# Changelog

## [1.5.1] - 2026-11-07

### Fixed

* **Kinetic charge** — effect icons now switch and clear correctly when conserved damage changes tier or is spent (no leftover icons until timer ends).
* **Drowsy / Magical Sleep** — no longer cancel when blocking with a shield or when hit by zero-damage projectiles (e.g. snowballs). Only real damage wakes the target.
* **Magical Sleep** — full immobilization for mobs and players (same control lock as Frozen / Paralyzed); ambient mob sounds while asleep are reduced.
* **Lullaby** — Drowsy correctly transitions into Magical Sleep when it expires.
* **Siren's Song** — only converts hostile monsters (villagers and animals are no longer enthralled).
* **Retribution / Psionic Blast** — damage values aligned with SPD (including self-damage and scaling with missing health / number of targets).
* **Corrupted / Enthralled**
  * Converted mobs drop loot and experience as if killed by the player (including “player kill only” drops).
  * Converted mobs no longer drop loot/XP again on death.
  * Vex summoned by a converted Evoker inherit the conversion but do not grant extra XP.
  * Iron Golems no longer keep attacking converted illagers after conversion.
  * Non-converted illagers can fight converted ones.
  * Converted raid members are removed from the raid progress bar.
* **Boss / miniboss immunity** — Warden and other miniboss/boss tags are immune to Dread, Enthralled, and Corrupted as intended.

### Changed

* Shared immobilization logic for Frozen, Paralyzed, and Magical Sleep.
* Team assignment for Corrupted and Enthralled centralized for clearer ally behavior.

### Known issues

* Purity still does not fully ignore slime / lava / water slowdown.

---

## [1.5.0] - 2026-11-06

### Added

**Scrolls** (read time ~1 second; craft with paper + item; exotics = basic scroll + lapis)

* **Scroll of Terror** — nearby hostiles gain *Terror* and flee (paper + bone).
* **Scroll of Dread** — stronger panic: *Dread* makes enemies flee and they may despawn (exotic of Terror).
* **Scroll of Teleportation** — long-distance teleport (avoids solid blocks, lava, and mid-air) (paper + ender pearl).
* **Scroll of Passage** — teleport toward the nearest village (exotic of Teleportation).
* **Scroll of Remove Curse** — removes curse enchantments from the item in the offhand (paper + diamond).
* **Scroll of Anti-Magic** — grants *Anti-Magic* (exotic of Remove Curse).
* **Scroll of Lullaby** — nearby creatures become *Drowsy*, then fall into *Magical Sleep* (paper + any music disc).
* **Scroll of Siren's Song** — *Charms* nearby foes and *Enthralls* the foe you look at (or the nearest one), turning it into an ally (exotic of Lullaby).
* **Scroll of Rage** — alerts hostiles in a wide area and applies *Amok* to those nearby (paper + redstone).
* **Scroll of Challenge** — alerts foes and creates a temporary 7×7 (smaller near bosses) *Challenge* zone (exotic of Rage).
* **Scroll of Retribution** — damages living creatures in vision (and any with Glowing, anywhere) based on the user’s missing health; applies *Blinded* and *Weakness* (paper + glowstone).
* **Scroll of Psionic Blast** — stronger Retribution-style blast that also hurts the reader; self-damage scales down with more targets hit (exotic of Retribution).

**Stones** (throwable; 2 stones from the matching scroll; snowball-like arc)

* **Stone of Fear** — applies *Terror* to the hit target (or nearest living entity in a small radius on block impact).
* **Stone of Blink** — teleports the thrower to the impact location.
* **Stone of Deep Sleep** — puts the target into *Magical Sleep* immediately.
* **Stone of Aggression** — marks the target so nearby hostiles attack it for a short time.
* **Stone of Blast** — explosion at the impact point (damages entities, does not break blocks).

**Effects** (new or fully wired for scrolls/stones)

* **Dread** — like Terror, plus possible despawn once out of the player’s vision. Bosses, minibosses, and static entities are immune.
* **Drowsy** — lead-in to Magical Sleep (from Lullaby); canceled if the entity takes real damage.
* **Magical Sleep** — full immobilize until woken by damage. Players and allies regenerate while asleep; the effect ends when fully healed.
* **Amok** — clears the current target and forces aggressive pathing toward the nearest creature; ignores hazards such as fire or precipices.
* **Challenge** — 33% less incoming damage; hunger/saturation do not decrease (eating still works).
* **Enthralled** — converts the target into a permanent ally (not player-commandable). Immune on bosses / minibosses / static.
* **Aggression (marked)** — forces nearby hostiles to target the marked entity for a few seconds.
* **Blinded (SPD)** — very short vision range; blocks use of scrolls while active.
* **Anti-Magic** — blocks magical damage and scroll reading for its duration.
* **Weakness** — reduces outgoing damage by 33%.

**Other**

* Boss / miniboss / static immunity to **Corrupted**, **Dread**, and **Enthralled**.
* Code reorganization (packages, shared proc/progression helpers, etc.).

### Known issues

* Purity still does not fully ignore slime / lava / water slowdown.
* Mind Vision block transparency still deferred (26.3 render pipeline).

---

## [1.4.3] - 2026-10-25

### Added

* **Entity classes** `#spd:fiery`, `icy`, `acidic`, `electric`, `inorganic`, `demonic`, `immovable`, `static`, `miniboss`, `boss`) — datapack tags + config so other mods can mark their mobs without a SPD update.

* **Enchantment Descriptions** — English and Spanish descriptions for all SPD enchantments.

* **Effect descriptions (JEED-ready)** — English and Spanish strings for SPD effects `effect.spd.<id>.description`).

 *Missing* *arrow item** translations in Spanish (Mexico).

### Changed

* **Golden Nest → Golden Beehive** — rename of block/item/entity references; recipe uses any beehive `#spd:beehives`). Textures still use the previous nest assets until new art is ready.

* **True Invisibility** — armor and held items are hidden (including first person); Camouflage refreshes the effect more smoothly.

* **Shocking** — chain lightning no longer damages the primary target (matches SPD); clearer lightning visuals on proc.

* **Haste / Stamina / Swiftness / Flow** — movement exhaustion is compensated so faster movement does not burn extra hunger.

* **Frost cloud** — slightly taller vertical volume.

* **Chilled/Frozen** feedback (particles, freeze/shatter sounds, powder-snow-style overlay).

* **Toxic gas / caustic ooze / storm & aqua vs fiery** — damage scales through the shared progression helper (ready when dungeon floors exist).

* **Corrupted / Doomed / Deferred Damage** — cannot be removed by milk or Cleansing; `/effect clear` still works.

* **Corrupted** — conversion sound and stronger visual feedback when a mob is corrupted.

### Fixed

* **Aqua Brew** — throwable model, water on solid blocks / waterlogging, and knockback.

* **Snap Freeze** — 3×3×3 area; does not shorten a longer Frozen duration; freeze sound on apply.

* Minor lang and creative-tab consistency for newer content.

### Known issues

* Purity still does not fully ignore slime / lava / water slowdown.

\---

## \[1.4.2\] - 2026-10-21

### Fixed

*   **Terror** — ranged mobs (skeletons, pillagers, etc.) now flee properly and stop shooting while terrified.

### Changed

*   **Jade** is no longer listed or shipped as an included/related mod.

\---

## [1.4.1] - 2026-10-21

### Added

**More weapon enchantments**

* **Venomous** *(uncommon)* — chance to apply Poison on hit, stackable.

* **Vorpal** *(rare)* — chance to apply Bleed on hit.

* **Eldritch** *(rare)* — chance to inflict Terror in nearby enemies (5s; shortens if the charmed target takes damage).

* **Crystal** *(rare)* — +33% bonus damage on hit; 33% chance to consume an extra durability point. Exclusive with Unbreaking.

**Armor enchantments** (slot-specific)

* **Obfuscation** *(common, helmet)* — reduces how far monsters can detect you (stronger at higher levels).

* **Swiftness** *(common, leggings)* — higher base movement speed while no monsters are nearby.

* **Flow** *(common, leggings)* — higher movement speed in water.

* **Viscosity** *(common, chest)* — defers a fraction of incoming damage into a **Deferred Damage**.

* **Potential** *(common, leggings)* — chance when hit to restore a little hunger/saturation.

* **Brimstone** *(common, chest)* — nullifies on-fire / in-fire damage and blocks Burning effect; campfire damage is halved. Does not protect against lava or magma blocks.

* **Entanglement** *(uncommon, boots)* — chance when hit to gain **Herbal Armor**.

* **Repulsion** *(uncommon, chest)* — chance to knock back nearby attackers.

* **Camouflage** *(uncommon, helmet)* — while fully covered by tall plants/vines, grants True Invisibility.

* **Affection** *(uncommon, helmet)* — chance when hit to Charm the attacker (10s; shortens if the charmed target takes damage).

* **Anti-Magic** *(uncommon, chest)* — raw reduction against magical damage; exclusive with Protection / Blast / Fire / Projectile Protection.

* **Bloody Thorns** *(uncommon, chest)* — chance to apply Bleeding to attackers.

**Related effects**

* **Herbal Armor** — reduce a little amount of incoming damage, expires after blocking certain total amount, lost if you jump or sprint.

* **Deferred Damage** — damage pool that is paid out over time; bypasses armor and enchantments.

* **Charmed** — cannot attack or target the charmer; duration drops when damaged.

* **Terror** — enemies flee and cannot attack while terrified; duration drops when damaged.

### Changed

 *Most on-hit weapon enchantments only attempt to proc when the attack is at least* *90% charged**.

* **Chilled** also slows attack speed.

### Fixed

* **Corrupting** no longer revives bosses on a successful proc.

* **True Invisibility** refresh (including Camouflage) no longer flashes the player visible; visual stealth is synced through vanilla invisibility.

\---

## [1.4.0-beta] - 2026-10-20

### Added

**Weapon enchantments** (compatible with melee and many ranged weapons)

* **Chilling** *(common)* — chance to apply Chilled on hit; stacks duration (cap 6s). Exclusive with Fire Aspect, Flame and Blazing.

* **Blazing** *(common)* — chance to apply Burning; bonus damage if the target was already burning. Exclusive with Fire Aspect, Flame and Chilling.

* **Shocking** *(common)* — chance to deal a portion of the hit as lightning damage to nearby enemies (skips the wielder, team, and pets).

* **Kinetic** *(common)* — excess damage on kills is conserved and added to the next hit; conserved damage decays over time.

* **Blocking** *(uncommon)* — chance on hit to gain a short absorption shield.

* **Projecting** *(uncommon)* — improves melee reach and ranged projectile travel.

* **Grim** *(rare)* — chance to insta kill. Probaility based on the target’s missing health (bosses take a reduced execute).

* **Vampiric** *(rare)* — chance to heal for a portion of damage dealt. Probaility based on the wielder’s missing health.

* **Corrupting** *(very rare)* — chance on a *killing blow* to convert the foe:

*Mobs become* *Corrupted** (fully healed, cleansed of debuffs, fight other monsters, join your team so pets and many ally-aware mods treat them as allies). Takes corruption DoT. Corruption is permanent, its effects only end in death.

**Related effects**

* **Corrupted** — ally conversion AI, team assignment, pet-friendly targeting.

* **Doomed** — boss-only alternative to corruption. +67% damage taken from all sources.

* **Kinetic Charge** — visual HUD icon reflecting conserved damage.

\\---

## [1.3.0] - 2026-10-15

### Added

\\- **Cold effects**

\- **Chilled** — movement slow scales with remaining duration (up to 50%); at 10+ seconds converts to **Frozen**.

\- **Frozen** — full lockdown (no voluntary movement/actions); ends on damage; can grant Chilled when it expires in water; cleared by fire/lava.

\\- **Frost family**

\- **Potion of Frost** (splash) — lingering frost cloud that applies and stacks Chilled; extinguishes fire.

\- **Potion of Snap Freeze** (splash) — instant freeze in a 3×3×3 area (longer duration in water); extinguishes fire/campfires.

\- **Elixir of Icy Touch** — *Imbued with Frost*: immunity to chill/freeze (and configurable damage/effects); melee applies stacking Chilled to enemies. Configurable immune entity list (Snow Golem by default).

\\- **Fire family**

\- **Potion of Liquid Flame** (splash) — ignites a 3×3×3 volume, applies **Burning**, lights campfires/candles/TNT, melts snow.

\- **Potion of Dragon’s Breath** (drinkable) — fires a cone of flame; applies Burning and **Crippled**.

\- **Elixir of Dragon’s Blood** — *Imbued with Fire*: fire/Burning immunity (lava partially reduced); chance to apply Burning on melee; scorches vegetation underfoot.

\\- **Poison family**

\- **Potion of Toxic Gas** (splash) — spreading toxic cloud (1 damage/s, poison-like).

\- **Potion of Corrosive Gas** (splash) — denser cloud; damage increase the longer you stay inside.

\- **Elixir of Toxic Essence** — *Imbued with Toxicity*: poison/toxic immunity; emits toxic gas while active.

\\- **Others**

\- **Potion of Paralytic Gas** (splash) — spreading gas cloud applying **Paralyzed** (lockdown; damage shortens duration).

\- **Potion of Shrouding Fog** (splash) — smoke cloud applying **Blinded** (mobs: 1-block detection & lose aggro; players: tight fog vision). Does not affect the thrower, their team, or their pets.

\- **Potion of Storm Clouds** (splash) — storm cloud that waterlogs and places flowing water, extinguishes fire, damages fiery mobs.

\\- **Brews**

\- **Infernal Brew** (splash) — spreading inferno gas: Burning, ignites flammables; cancelled by Blizzard cells.

\- **Blizzard Brew** (splash) — spreading blizzard gas: heavy Chilled, extinguishes fire; cancelled by Inferno cells.

\- **Shocking Brew** (splash) — fixed 7×7×3 field for 20s: lightning damage + Paralyzed each second.

\- **Caustic Brew** (splash) — instant 7×7×3 **Caustic Ooze**. Brewed from Toxic Gas + slime ball.

\- **Aqua Brew** (throwable item, stack 16) — geyser: flowing water on solids, extinguish fires, knockback \\\\\\\~2 blocks, damages fiery mobs. Craft: Storm Clouds splash + lapis block → 8.

\\- **Armor elixirs / potions**

\- **Potion of Earthen Armor** — **Barkskin**: random 0-level physical damage reduction; level steps down over time.

\- **Elixir of Arcane Armor** — **Arcane Armor**: random 0-level magic damage reduction; same step-down logic as Barkskin.

\\---

## [1.2.0] - 2026-10-11

### Added

\\- **New potions**

\- **Potion of Experience** — grants experience 1 level of experience.

\- **Potion of Haste** — large movement-speed boost; stacks with vanilla Speed.

\- **Potion of Stamina** — movement-speed boost weaker than Haste, but also more durable; stacks with vanilla Speed and Haste.

\- **Potion of Purity** — *Purification Barrier*: blocks environmental damage and slows (fire, freeze, lightning, cobwebs, powder snow, etc., configurable).

\- **Potion of Cleansing** — removes harmful effects and briefly blocks new ones (whitelist / blacklist configurable).

\- **Potion of Levitation** — fixed-height hover: horizontal movement only.

\- **Elixir of Might** — vanilla Strength plus a **Max Health Boost**.

\- **Potion of Invisibility** — true invisibility: enemies lose and cannot retarget you; attacks are critical; ends when you attack or fire a bow/crossbow/trident.

\- **Potion of Mind Vision** — applies Glowing to hostile mobs in a large radius.

### Changed

\\- “Mother” (logic) healing effects remain hidden; only GUI effects show in the inventory HUD.

\\---

## [1.1.0] - 2026-10-10

### Added

\\- **Golden Bee**

\- New friendly flying mob.

\- Wild: patrols and defends nearby hive.

\- Tameable with Honeyed Healing.

\- Commands while tamed: Stay, Wander, Work, Follow.

\- Work mode: pollinates flowers and delivers nectar only to a **Golden Nest**.

\- Custom sting that does not kill the bee after attacking; immune to poison.

\- Can breed with other Golden Bees or vanilla bees (Golden × Golden → Golden; Golden × vanilla → 50% chance).

\- Rare natural conversion chance from vanilla bees on spawn.

\\- **Golden Nest**

\- Custom hive block for Golden Bees (and vanilla bees (WIP)).

\- Up to 6 occupants and up to 10 honey levels.

\- Does not anger nearby bees when broken or harvested (even without a campfire).

\- Keeps stored bees and honey when broken even without silk touch.

\- Crafting: surround a **bee nest** or a **beehive** with gold nuggets.

### Changed

\\- Potion “mother” effects (logic/pool) are hidden from the inventory and HUD; only the visual GUI effects are shown.

\\- Minor polish to potion tooltips and creative tab presentation.

### Fixed

\\- Brewing conversions keep the correct family, potency, duration, and container type (drinkable / splash / lingering).

\\---

## [1.0.0] - 2026-10-09

### Added

\\- Initial release.

\\- **+4 New potions**

\- **Potion of Regenerative Healing**: A powerful healing potion. It provides a strong regeneration effect that gets weaker over time. Obtainable by combining a Potion of Healing and a Potion of Regeneration in a brewing stand.

\- **Elixir of Honeyed Healing**: Provides the same effect as **Regenerative Healing** and also restores a small amount of hunger and saturation. Obtainable by combining a Regenerative Healing potion with honey or honeycomb.

\- **Elixir of Aquatic Rejuvenation**: Provides regeneration while the player is in water or under rain. No healing is wasted — the effect pauses when the player is at full health or out of the water. Obtainable by combining a Regenerative Healing potion with a prismarine shard or prismarine crystals.

\- **Potion of Shielding**: Grants a temporary shield that decays over time. The more shield you have, the faster it decays, but it generally lasts a decent amount of time. Obtainable by combining a Regenerative Healing potion with lapis lazuli.

\\- Variants: basic, extended (redstone), powered (glowstone), powered+extended, and powered II, including splash and lingering forms.

\\- Localization: 2 languages (English, Spanish).