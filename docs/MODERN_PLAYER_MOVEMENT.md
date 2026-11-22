# Modern player movement

EFR Plus owns sprint swimming, crawling and low-clearance crouching on Forge 1.7.10. The reference target is Minecraft Java 1.21.11. The movement feature is opt-in for this candidate.

## Server settings

Set these under `"backported features"` in `config/etfuturum/mixins.cfg` and restart the server. For a local world, restart the game.

| Setting | Default | Behavior |
| --- | --- | --- |
| `modernPlayerMovement` | `false` | Enables the 1.5-block crouch, 0.6-block crawl and sprint-swim poses. |
| `legacySneakInModernMovement` | `false` | With movement enabled, keeps the 1.7.10 sneak camera, pick ray and 1.8-block hitbox in open space. Clearance can still force a smaller pose when the player is already under a ceiling. Entering a 1.5-block opening from standing requires the modern sneak mode or an approach in a lower pose. |

A movement-enabled server advertises its mode in EFR login protocol v3. A matching client uses the server's mode for prediction even if its local config differs. Older clients must update before joining a movement-enabled server. Disabling the feature preserves the previous EFR protocol versions and height negotiation. The pose byte uses player DataWatcher slot 29 and vanilla spawn/entity-metadata packets. There is no client pose request. The server resolves poses using its collision, water and sneak/sprint state, and the owning client predicts from its movement input. Positions and movement corrections still follow the existing Forge packet path. Remote actors, including replay actors, read the recorded pose byte. The metadata carries only the pose.

## Collision, aim and bridging

The local camera and pick ray use the same physical eye while crouched or horizontal. The server uses that eye for the player eye getter and the existing dig-distance calculation. The player's normal stance packet and the block-placement validation remain on the existing 1.7.10 path. The movement feature uses a single immediate eye transition with no added camera interpolation. The vanilla sneak input, edge-stop algorithm, and sneak slowdown, including EFR's existing Swift Sneak modifier, still run. Low-clearance crouching slows the local player without generating a shift press or changing the sneak flag. Backlytra retains the glide body, eye and hitbox.

In the Forge loopback fixture, disabled and legacy-sneak clients had a 1.80-block collision box and a settled client eye/pick origin of 1.54 blocks above the floor while holding shift. The server's legacy eye was 1.62. Modern crouch measured 1.50 blocks high and 1.27 at both client pick and server eye. The same flat platform stopped the player at approximately X=1.30 and accepted edge placement in all three modes. The 0.27-block client pick-origin difference changes the angle needed to hit a face. The legacy-sneak fallback preserves the measured 1.7.10 aiming geometry.

## Reference and companion audit

Thanks to Grihail for [Modern Poses at 641795f](https://github.com/Grihail/Modern-Poses/tree/641795f017a8fa1fd2d0b55c1be307b1d34aa208). Its `client/ClientPoses.java`, `server/ServerPoses.java`, `mixins/MixinEntity.java`, and `client/PoseCamera.java` were reviewed for pose order, collision checks, small-box water detection and camera math. The CurseForge 1.3.1 release requires Forge 1614 and UniMixins on both sides. Its published scope includes offhand, combat, weapon stats, armor, potion and food changes. The source's independent mixin configuration arrived after the 1.3.1 version commit `9da23477b2419f0adac2046fa0d3184e9fba5d39`, in `aa4fb6d` and later. EFR Plus uses its existing login protocol and vanilla metadata, with no Modern Poses dependency or imported API, combat or inventory code. Mojang's 1.21.11 `Player.updatePlayerPose` and `Player.travel` bytecode were checked for the pose fallback order and pitch-based swimming movement. [Modern Poses release](https://www.curseforge.com/minecraft/mc-mods/modern-poses) · [Minecraft Java 1.21.11](https://www.minecraft.net/en-us/article/minecraft-java-edition-1-21-11)

| Companion source reviewed | Contract |
| --- | --- |
| Angelica WDG `9b7d2dd4` | `compat/mojang/Camera.update` gets the view entity eye. The shaders' `MixinRenderPlayerArmor` wraps armor pass setup and the cape, and `MixinRenderNameTag` handles shader draw state. EFR applies the horizontal transform inside the player model/equipment matrix. |
| HBM Space WDG `bab059951` | `ModEventHandlerRenderer` uses armor passes, attachment anchors on the biped, and `RenderPlayerEvent.Specials` for held gear. Riding has separate render hooks. The pose hands the matrix to these passes and yields to mounts. Eye-based item aim uses the player getter. |
| FMUR WDG `6ae086c` | `PlayerEquipmentClientHandler.renderStack` renders face and night-vision equipment in `Specials.Post`. `TickHandlerClient` and gun paths read the eye getter. `FlansModClient` enforces its infantry camera setting, and `PlayerLodRenderer.replaceNative` can substitute distant actors. Native actor presentation and FMUR's distant proxy presentation have different pose data. |
| ReplayMod WDG `0b502a2d` | `RecordingEventHandler.spawnRecordingPlayer` writes spawn and equipment packets. Vanilla `EntityTrackerEntry` sends changed metadata to the tracked player and observers. `PacketListener` records that stream. `CameraEntity` uses a separate spectator camera and can set the tracked actor as the active view. `Mixin_HideNameTags_LivingEntity` controls nameplate visibility separately. |
| WGQOL `61787a2` | `KeyProfiles` and `ProfileMovement` preserve held input during profile swaps. The weather camera hooks observe the active render view. |
| WGCore `0a5646f` | `NameplateOverlayRenderer` handles `RenderLivingEvent.Specials.Pre` and reads vanilla sneaking for range. Pose packets carry no names, UUIDs, teams or scoreboard values. |
| WGEvents `24989a4` | `TeamService` owns scoreboard membership and team display fields. `ModeZombies` checks the ordinary sneak flag for barricade repair. |
| lwjgl3ify-WDG `d1361fc` | `Lwjgl3ifyRelauncherTweaker` selects and launches the bundled Java runtime. The EFR renderer uses the existing LWJGL matrix calls. |

## Reproducing the Forge fixture

The opt-in `scripts/movement_tests.gradle` init script compiles the fixture separately from the release JAR. Give `runServer` and each `runClient` separate disposable working directories with `-PmovementGameDir=/absolute/path`. The default loopback port is 25578, override with `-PmovementPort`. Use a flat disposable world, localhost, spawn protection off, and enable the mode in the test server's mixins config. Start `runClient --username=PoseDriver` with `-PmovementFixture=modern`, `legacy` or `disabled` to match the server setting. A second client with `-PmovementFixture=observer` and a distinct username verifies remote pose and hitbox changes. Set the client's music to 0 and master volume to 15% before starting. A `FIXTURE_STOP` marker in the disposable working directory stops that process. Successful runs log `MOVEMENT_FIXTURE_CLIENT PASSED` and, for the observer, `MOVEMENT_FIXTURE_REMOTE PASSED`.

Verified in the dev fixture against a dedicated Forge 10.13.4.1614 server over loopback TCP using Java 8. Coverage includes shift toggling, stopping at a flat edge, a stationary side-face pick and confirmed placement, diagonal corner stopping, a server position correction while still holding shift, upside-down slab clearance, releasing shift under the slab, and a stationary ray into the slab underside, crawl exit from a forced one-block gap, sprint-swim entry and release, contained water between waterlogged slabs, remote hitboxes, Elytra takeoff through landing, and reconnect with the server mode reapplied. Legacy mode intentionally blocks walking from the open into the overhead slab with the legacy hitbox. The 1.7.10 baseline uses the disabled mode on the same world and packet path. A separate X35 client disconnected before joining the movement-enabled server. The 256-height fixture exchanged a legacy-width block-effect packet after the position correction. The old-height and both movement-mode profile round trips were also checked at 256 and 400 world height by `./gradlew --offline check build --no-configuration-cache`.

## Pack verification

The JAR is `etfuturum-3.9.0_X39.jar`. The filename and embedded mod version match. Its SHA-256 is `63a2d0c4cd12f31c3c26fb0fd3cc3c0aecb6d1fd30ce77fd6859435c950c5d25`.

The Wargames Event 2.0.16 copy ran the reobfuscated JAR on bundled Java 21.0.11 with lwjgl3ify `d1361fc`, Angelica X15, HBM Space WG2.5.4, FMUR X230, ReplayMod X77, WGCore X230 and WGQOL X84. It used a fresh 384-height integrated world. The fixture rendered HBM hazmat equipment with a held FMUR M4A1 in crouch, and FMUR Mk7 helmet, BDU top and IOTV with a held HBM mask in crawl. It captured first- and third-person views. The fixture allowed third-person inspection locally because FMUR's normal infantry policy can restrict it.

ReplayMod X77 recorded and played all four poses, including both equipped head items, the recorded player name, a test scoreboard prefix, and the spectated swim view with a 0.40-block pick origin. The older X55 from the Release Testing copy rejected 384-height `S21PacketChunkData` during recording. X77 already includes the extended-header normalizer. Neither companion required a source edit.

For the HUD check, the Java 21 fixture kept the same HBM helmet and FMUR item across dry standing, crouching, crawling and release. Forge delivered identical projection and model-view matrices for the helmet, crosshair, armor, health, hunger, hotbar and XP. The FMUR ammo panel's top edge stayed at framebuffer Y852 from X1436 to X1887 in all three 1920×1080 captures. The world camera and held-item view remain free to respond to pose, view bobbing and item animation. WGQOL computes its profile strip from the current right-hand HUD stack. When the crawl eye entered water, Forge's right stack changed from 49 to 59 and WGQOL's profile strip moved up 10 scaled pixels to make room for the air bubbles. The fixed HUD transforms still matched.

The remaining pack checks are manual shift-bridge timing under WAN latency, additional slab and stair shapes, shaders and scopes, mounted gear, FMUR distant-player proxies, and live WGCore/WGEvents team overlays. The fixture covers vanilla player tracking and the selected Java 21 integrated pack combinations.
