<img width="574" height="574" alt="DF" src="https://github.com/user-attachments/assets/34b761d3-c14b-4a3b-8ac8-319fe5aeac91" />

Dynamic Felling
=======
Dynamic Trees makes trees grow and fall like real trees, but chopping one down doesn't feel very Dynamic™: hold the button and it breaks. Introducing Dynamic Felling! This is a companion mod for Dynamic Trees, and I wanted to add the touch of realism that was missing there, so felling a tree feels like actually chopping it down with an axe.

[CurseForge](https://www.curseforge.com/minecraft/mc-mods/dynamic-felling) / 
[Modrinth](https://modrinth.com/mod/dynamic-felling)(not approved yet)

<h2>Updates</h2>

<details>
<summary><b>1.3.0</b>: Sharpening (latest)</summary>
<ul>
<li>Axe sharpening implemented: Axes now have an edge that wears down from 100 to 0 as you chop. A keen axe fells trees in 25% less swings, a dull one takes 25% more. The tooltip shows the sharpness of the edge, and a small icon sits next to the swing counter.</li>
<li> Hold smooth stone in your main hand and six sand in your offhand, then hold right-click to grind the stone into a Whetstone. Works with modded smooth stone and sand (<code>c:stones/smooth</code>, <code>c:sands</code>).</li>
<li> Hold a whetstone in your offhand and hold right-click to sharpen your axe, adding 25 points to the edge after sharpening each side once. Full animations in first and third person.</li>
<li> Swing formula adjusted for axe speed. Faster axes now swing faster. Slow axes keep a quick swing but pause longer between hits.</li>
<li>New config: turn timed felling off entirely (<code>enabled</code>), set an axe's tier by hand (<code>axeTierOverrides</code>), and tune edge wear and whetstone strength.</li>
</ul>
</details>

<details>
<summary><b>1.2.0</b>: Compatibility</summary>
<ul>
<li>Falling Leaves and Dynamic Tree Physics support.</li>
<li>New swing formula and an on-screen swing counter (client config).</li>
</ul>
</details>

<details>
<summary><b>1.1.0</b>: Jade</summary>
<ul>
<li>Jade shows the swings left to fell a trunk, live.</li>
<li>Fixed a camera spin bug.</li>
</ul>
</details>

<details>
<summary><b>1.0.0</b></summary>
<ul>
<li>First release.</li>
</ul>
</details>

## Features

- **Real chopping:** Axes chop Dynamic Trees trunks in timed swings instead of mining them. Thicker trunks take more swings; better axes and Efficiency lower the swings needed. Haste and Mining Fatigue change the pace.
- **Chop animations:** Full swing animations in first and third person, with varied swing angles in first person. Left-handed players swing mirrored.
- **Impact and Feel:** Each hit cracks the trunk, sprays wood chips from the side you struck, shakes leaves loose from the tree's own canopy and gives the camera a small jolt. Sounds get deeper and heavier on thicker trunks.
- **Saved chopping progress:** If your chopping gets interrupted, the cuts stay in the trunk for a minute, so you (or a friend) can finish the job.
- **New Hazards:** Chopping a tree with a bee nest in it can anger the bees (unless there's a campfire underneath). Trees fall away from you, but not always. Sneaking no longer picks the direction, so keep an eye out ;)
- **Committed swings:** You slow down while chopping, can no longer sneak, and you can only chop from close range.
- **Multiplayer/Co-op:** Chop a trunk together and every hit counts toward felling it, so two players bring a big tree down in half the time. Other players see your swing, hear your chops, and see the chips and falling leaves.
- **Compatibility:** Works with modded axes, Dynamic Trees addons (including Dynamic Trees Plus), and has a config to exclude tools such as chainsaws.

## Demo

[![Dynamic Felling demo](https://img.youtube.com/vi/ys0AQpgfMpc/maxresdefault.jpg)](https://www.youtube.com/watch?v=ys0AQpgfMpc)

## Optional Compatibility

- [Jade](https://www.curseforge.com/minecraft/mc-mods/jade): Shows a live count of the swings left to fell a trunk.
- [Falling Leaves](https://www.curseforge.com/minecraft/mc-mods/falling-leaves-forge): Chopping shakes loose Falling Leaves' leaf particles in place of the default ones.
- [Dynamic Tree Physics](https://www.curseforge.com/minecraft/mc-mods/dynamic-trees-physics): Fallen trunks and branches can be chopped up with swings, with a configurable swing multiplier (`fallenSwings`).


## Requirements

- Minecraft 1.21.1
- NeoForge 21.1+
- [Dynamic Trees](https://www.curseforge.com/minecraft/mc-mods/dynamictrees): required on client and server
- [Player Animator](https://www.curseforge.com/minecraft/mc-mods/playeranimator): required on the client

## Configuration

Server settings are in `config/dynamicfelling-server.toml`: swings per axe tier, Efficiency bonus, max swings, chop reach, backfall chance, bee anger chance, fallen swings, and excluded tools.

Client settings are in `config/dynamicfelling-client.toml`: Swing counter on or off.

## License

All Rights Reserved. See [LICENSE](LICENSE).
