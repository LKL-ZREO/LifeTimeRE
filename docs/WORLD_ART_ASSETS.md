# 世界页演示美术素材

日期：2026-11-08

本轮使用内置 imagegen 生成五张演示位图，并复制到 `app/src/main/res/drawable-nodpi/`：`world_camp_art.png`、`world_tent_art.png`、`world_workshop_art.png`、`world_path_art.png`、`world_traveler_art.png`。它们是概念美术与 UI 演示素材；四张场景是单张平面图，无法直接制作逐帧角色动作或物件遮挡动画。正式制作时应请美术拆成背景、建筑、前景、可点击物件、光效和角色精灵图层。

## 最终提示词

### 营地

> Use case: stylized-concept. Asset type: full-screen background art for an Android portrait game world screen. Create a polished high-detail Japanese RPG pixel-art campsite at dusk, 16-bit / 32-bit era inspired but with modern dense pixel craftsmanship and crisp intentional pixel clusters. A small canvas tent on the left, an inviting bright campfire at lower center, a wooden workshop with a glowing window on the right, a narrow trail and signpost leading toward distant mountains in the upper middle. Layered trees and foliage, tiny props, warm fire lighting, rich teal night shadows, gold accents. Orthographic three-quarter game view, all places clearly separated and easy to tap. Fill the entire tall portrait frame edge to edge; the bottom half must contain detailed ground, path, foreground plants and props, not empty space. No character, no text, no labels, no menus, no border, no watermark. Every feature crisp and pixelated; do not smooth or blur; not a mockup of a phone.

### 帐篷（以营地图作风格参考）

> Use case: stylized-concept. Asset type: full-screen Android portrait game background, companion scene to the provided Japanese RPG pixel-art campsite reference. Create the INSIDE of the same traveler's canvas tent, high-detail finely crafted pixel art, consistent palette and 3/4 orthographic viewpoint. Cozy bedroll and folded blanket on the lower left, travel chest and journal, a clearly visible empty wooden memory shelf on the center-right wall designed as an interactive hotspot, warm hanging lantern, canvas seams, small window showing the same sunset mountains. Fill entire tall portrait frame with richly detailed interior floor and props down to the bottom edge. No people, no text, no labels, no UI, no border, no watermark. Crisp deliberate pixel clusters, no blur or smooth painterly gradients.

### 工坊（以营地图作风格参考）

> Use case: stylized-concept. Asset type: full-screen Android portrait game background, companion scene to the provided Japanese RPG pixel-art campsite reference. Create the INSIDE of the workshop shown in the reference: beautifully detailed traditional wood-and-stone craft room, 3/4 orthographic game view, large interactive workbench at center and lower middle with three visibly different materials (small wood plaque, parchment, folded red cloth), tools and anvil, shelves, warm lantern, a window showing the same twilight mountain valley. Rich finely clustered pixel art like a premium Japanese RPG, consistent palette with the campsite. Fill the tall portrait frame edge to edge with detailed wooden floor and props; no blank bottom half. No people, no text, no labels, no UI, no border, no watermark. Crisp intentional pixel clusters, no blur.

### 小径（以营地图作风格参考）

> Use case: stylized-concept. Asset type: full-screen Android portrait game background, companion scene to the provided Japanese RPG pixel-art campsite reference. Create the trail and lookout beyond that campsite, same mountain valley and sunset, a winding stone path running from the bottom foreground through a grassy overlook to the distant view, a weathered wooden signpost near center, three distinct stepping stones/footprints along the path as interactive points, a small river and waterfall, pine trees, wildflowers, ruined stone marker, warm distant castle lights. Rich high-detail Japanese RPG pixel art, 3/4 orthographic game view, consistent palette with reference. Fill the entire tall portrait frame edge to edge, including detailed foreground in the bottom half. No people, no text, no labels, no UI, no border, no watermark. Crisp deliberate pixel clusters, no blur or smooth painterly rendering.

### 旅人（透明背景）

> Use case: stylized-concept. Asset type: transparent character sprite for a portrait Android Japanese RPG pixel-art game. Single full-body young traveler standing in a relaxed three-quarter pose, teal cloak, warm brown leather backpack, small boots, readable silhouette, richly detailed crisp hand-placed pixel art matching a premium 16-bit/32-bit Japanese RPG environment. Full character visible with margin, centered, front-facing slight angle. Transparent background with real alpha, no shadow plane, no scenery, no text, no UI, no watermark, no soft antialias blur.

## 后续采购或委托美术时的交付要求

以这五张图作为构图与氛围参考，要求提交可编辑源文件（例如 Aseprite 文件）、统一像素网格与调色板、分层场景、角色动作帧、点击/高亮状态和移动端安全区预览。合同中明确游戏内商用、修改、二次制作和分发权限。先制作一幅营地与一个工坊互动的完整切片，确认手绘像素精度和动画成本，再扩到另外两个场景。

## 全应用统一美术（同日追加）

继续使用内置 imagegen，以 `world_camp_art.png` 为风格参考，生成并保存到 `drawable-nodpi`：`page_today_art.png`、`page_character_art.png`、`page_history_art.png`、`page_journal_icon.png`、`page_nav_atlas.png`。设置页复用工坊场景，旅人登记复用角色页场景。导航图标图集在 Compose 绘制时按四象限取用，原始文件保持完整。

### 今日页提示词

> Use case: stylized-concept. Asset type: Android portrait app page background for the Today journal screen, matching the provided premium Japanese RPG pixel-art campsite style. A close-up rustic travel writing desk near the campfire at dusk, leather-bound blank journal, ink bottle, quill, folded map, warm lantern, subtle tent canvas and forest in the distance. Rich hand-crafted fine pixel clusters, teal shadows and amber highlights, 3/4 orthographic perspective. Composition: strongest scene details at upper third and around edges, central lower area relatively dark and quiet for translucent app form cards, but still textured; fill entire tall portrait frame. No legible text, no symbols resembling UI controls, no people, no app screenshot, no border, no watermark; crisp intentional pixels, no blur.

### 角色页提示词

> Use case: stylized-concept. Asset type: portrait Android app page background for a character profile in the same premium Japanese RPG pixel-art world as the supplied campsite reference. A small traveler guild alcove at dusk: carved stone arch, hanging teal cloak, brass crest, wooden equipment stand, softly glowing lantern, forest and mountains visible through an open arch. Rich finely crafted pixel art and warm gold / teal palette, 3/4 orthographic game view. Upper third has decorative scene; middle and lower half retain quiet dark textured stone/wood areas for translucent profile and attribute cards. Full tall portrait frame edge to edge, no people, no writing, no text, no UI, no border, no watermark, crisp pixel clusters, no blur.

### 回顾页提示词

> Use case: stylized-concept. Asset type: portrait Android app page background for a journey history and weekly review screen, matching the supplied premium Japanese RPG pixel-art campsite. An intimate travel archive in a stone-and-timber lookout: shelves of journals and scrolls, pinned route map, compass, warm lantern, window toward the same twilight mountain valley and distant lights. Detailed finely crafted pixel art, teal shadows and amber highlights, 3/4 orthographic game view. Put the rich archive scene in the upper third, leave the middle and lower frame as quiet dark textured wood and stone to support translucent timeline cards. Full tall portrait frame edge to edge, no readable text, no people, no UI, no border, no watermark, crisp intentional pixel clusters, no blur.

### 旅志图标提示词

> Use case: stylized-concept. Asset type: transparent inventory icon for an Android Japanese RPG pixel-art app. One closed leather travel journal with brass corner clasps, small gold compass emblem, ribbon bookmark, viewed in three-quarter perspective. Premium fine deliberate pixel art matching the supplied camp scene: warm leather, aged parchment edges, gold metal highlights, crisp silhouette readable at 96dp. Centered with margin. Real transparent alpha background, no scenery, no floor, no text, no watermark, no UI frame.

### 导航图集提示词

> Use case: stylized-concept. Asset type: transparent 2-by-2 icon atlas for an Android Japanese RPG pixel-art app navigation bar, matching the supplied premium campsite art. Exactly FOUR distinct square inventory icons, one in each quadrant with generous empty margin and no overlap: top-left a small leather travel journal with quill (Today); top-right a teal-cloaked traveler head-and-shoulders portrait (Character); bottom-left a glowing campfire and small tent silhouette (World); bottom-right a rolled map and archival scroll with compass (Review). Each icon is self-contained, high-contrast finely crafted crisp pixel art, consistent gold/teal/leather palette, readable when reduced to 36dp. Equal size and alignment in a clean 2x2 grid. Real transparent alpha background, no text, no labels, no buttons, no frames, no watermark, no scenery.

## 篝火互动切片（同日追加）

第一版使用内置 imagegen 生成 `world_camp_unlit_art.png`（以原营地图为编辑目标）与透明图层 `world_fire_flame_art.png`（以原营地图为风格参考）。第一版直接叠加单张火焰，遮挡和像素锚点不够自然；现已由下方的分层帧动画取代。素材保存在 `drawable-nodpi`，原 `world_camp_art.png` 未被覆盖。

### 未点燃营地编辑提示词

> Use case: precise-object-edit. Asset type: replacement Android portrait game background for an interactive campfire scene. Input image: edit target, existing premium Japanese RPG pixel-art campsite. Change ONLY the central campfire flames and floating sparks near the lower middle: extinguish the fire completely, leaving the exact same stacked logs, stone ring, hanging cauldron, tripod and surrounding ground in place, with natural unlit dark wood and ash. Preserve every other pixel-art composition element, perspective, colors, positions and dimensions: tent, workshop, mountain, path, trees, lanterns, foreground, sky. Keep the scene richly detailed with crisp intentional pixel clusters and no smoothing. Do not change the size or aspect ratio. No text, no new objects, no UI, no watermark.

### 透明火焰图层提示词

> Use case: stylized-concept. Asset type: transparent animated-layer sprite for the central campfire in the provided premium Japanese RPG pixel-art campsite. Input image: style and lighting reference only. Subject: ONLY one vivid campfire flame cluster, about three distinct upward tongues of golden-yellow, orange and red flame with a few tiny detached ember pixels immediately above it. No logs, no stone ring, no tripod, no cauldron, no ground, no shadow plane. Composition: a single upright flame silhouette centered, tightly framed with transparent margin, suitable for placing over the existing pile of logs beneath the hanging cauldron; flame shape wider at base and tapered at top, straight-on 3/4 game view. Crisp intentionally clustered high-detail pixel art, same warm palette and pixel density as the reference campsite, not smooth painterly or vector. REAL transparent alpha background, no checkerboard, no text, no UI, no border, no watermark.

## 分层篝火帧动画

最终演示新增 `world_fire_frames_art.png`，为透明 2×2 四帧图集。`CampfireScene.kt` 在原图 1024×1536 的坐标系内绘制未点燃背景、火焰帧、从同一背景像素重绘的吊锅/支架/木柴前景遮挡、暖光和火星。`PixelSceneLayers.kt` 提供可复用的裁切映射、图集取帧和帧时钟。点击区也按原图坐标映射，随设备画面裁切保持对齐。当前四帧经过代码中的少量锚点补偿；正式美术交付应统一每帧底部锚点并提供分层源文件。

### 四帧火焰图集最终提示词

> Use case: stylized-concept. Asset type: transparent 2x2 sprite atlas for a looping campfire animation in a Japanese RPG pixel-art Android scene. Input image 1 is the exact campsite style and scale reference; input image 2 is the current flame concept reference. Create EXACTLY FOUR separate frames in a clean 2-by-2 equal-cell grid on real transparent alpha. Each cell contains the SAME compact orange-gold campfire rooted at the identical bottom-center anchor; vary only the upper flame tongues modestly from frame to frame so the loop reads as one living fire, not four different fires. The campfire should match the fine intentional pixel clusters and lighting palette of input image 1, with intricate yellow-white core and red-orange edges; narrower and less cartoon-like than input image 2. Large transparent margin within each cell, no flame or ember crosses cell boundaries. No logs, no stones, no cauldron, no tripod, no ground, no glow background, no text, no labels, no borders, no UI, no watermark, no checkerboard. Crisp hard-edged pixels, no blur or smooth painterly gradients.

## 营地环境动效

最初的环境演示由 `CampAmbientEffects.kt` 用代码绘制瀑布水纹、水花、涟漪、帐篷和工坊灯光及烟囱炊烟。水纹叠加方案现已由下方的整股瀑布逐帧图层替代；水花、灯光和炊烟仍保留。环境动画只在营地场景绘制，篝火点击状态和用户手动调整过的火焰坐标保留。

## 整股瀑布逐帧图层

内置 imagegen 生成 `world_camp_waterfall_clean_art.png`（移除近处瀑布的场景清底）和透明 2×2 四帧图集 `world_waterfall_frames_art.png`。`CampAmbientEffects.kt` 只在近处瀑布区域使用清底图，其余场景继续使用原 `world_camp_unlit_art.png`；然后绘制整股水形的四帧变化、前景岩石与帐篷顶边遮挡、水花和涟漪。原营地图及用户修过的篝火分层没有被覆盖。图集每帧顶部和底部锚点由代码保持一致，播放周期为每帧 190 毫秒。

### 瀑布清底编辑提示词

> Use case: precise-object-edit. Asset type: clean-plate reference for a layered animated waterfall in an Android Japanese RPG pixel-art campsite. Input image: edit target, the existing portrait campsite background. Change ONLY the large near waterfall on the left cliff, roughly x=110..330 and y=360..570 of this 1024x1536 image. Remove the falling white-blue water sheet and splashing water at its base; reveal a natural dry layered dark rock cliff behind where the waterfall was. Keep the pool and stream below, all surrounding rocks, plants, tent, workshop, mountains, sky, tripod, logs and every other composition element as close to the original as possible. Preserve exact portrait aspect ratio and resolution. Crisp high-detail intentional pixel clusters matching the source. No people, no text, no UI, no watermark.

### 瀑布四帧图集提示词

> Use case: stylized-concept. Asset type: transparent 2x2 frame atlas for the LEFT-CLIFF WATERFALL in the supplied Japanese RPG pixel-art campsite. Input image is a style and precise shape reference: recreate the same near waterfall at the left, NOT the distant waterfall. Exactly FOUR equal cells in a clean 2-by-2 grid; each contains one complete waterfall water silhouette rooted at the identical top lip and bottom pool anchors, with a broad white-blue sheet falling vertically then curving into a small foamy lower cascade. Change the contour, inner ribbons and foam naturally between frames so the WHOLE water shape moves, while its overall width, position and top/bottom anchors remain consistent. Show WATER ONLY: no rocks, cliff, trees, plants, pool background, ground, shadows, phone UI, lettering or borders. Dense hand-crafted crisp pixel clusters at the same detail level as the input, cool cyan/teal/white palette, no blur or smooth gradients. Real transparent alpha background and generous margin between cells; no overlap across cell boundaries.

## 工坊整图循环

`world_workshop_art.png` 是最初的工坊底图。以它为参考生成三张完整画面：`world_workshop_frame_1.png`、`world_workshop_frame_2.png`、`world_workshop_frame_3.png`。之后又在相邻画面之间用内置 imagegen 生成六张完整过渡画帧 `world_workshop_bridge_01.png` 至 `world_workshop_bridge_06.png`。当前循环播放九张生成画面；初始底图与第一张生成帧之间纹理差异较大，所以初始底图不参与循环。播放顺序从第一张过渡帧前进到 `world_workshop_frame_3.png`，再反向返回。

每对相邻原画帧占 225 毫秒：先显示清晰原画 135 毫秒，再用两步短渐变接入下一张。完整循环约 3.6 秒，显示更新间隔为 45 毫秒。素材本身包含不同形态的炉火、窗外瀑布、水桶涟漪、悬挂植物和灯火。选材料和制作时的短动画继续叠加在整图循环之上。

这些帧是逐张生成的，木纹、石纹等静止区域仍存在细节漂移。六张新增帧是真实的完整画面素材；短渐变只用于缓和切换，期间会有轻微重影。当前素材用于验证整图动画的观感；若用于正式版本，应将固定结构锁定，逐帧绘制会动的区域，再导出完整背景帧。
