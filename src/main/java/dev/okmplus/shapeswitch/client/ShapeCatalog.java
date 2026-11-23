package dev.okmplus.shapeswitch.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.network.chat.Component;

/**
 * 当前所有可轮换的挖掘形状。
 *
 * <p>通过 {@link OkmBridge} 反射读取 OKM 的形状注册表，因此其它附属模组注册的
 * 自定义形状也会自动出现，并且不依赖 OKM 的具体发布包（universal / neoforge / dev）。</p>
 */
public final class ShapeCatalog {

    /** 一个可轮换的形状条目。 */
    public record Entry(String id, String translationKey) {

        /** 本地化显示名；OKM 没有对应语言项时退化为 id 的可读形式。 */
        public Component displayName() {
            return Component.translatableWithFallback(translationKey, humanize(id));
        }
    }

    /** HUD 每帧都会问一次，避免每帧重建列表。 */
    private static List<Entry> cached = List.of();
    private static String cachedSignature = "";

    private ShapeCatalog() {
    }

    /**
     * 按 OKM 的注册顺序返回全部形状（已过滤配置中排除的 ID）。
     *
     * <p>配置或形状注册表变化时自动重建缓存。</p>
     */
    public static List<Entry> entries() {
        if (!OkmBridge.available()) {
            return List.of();
        }
        List<?> shapes = OkmBridge.registeredShapes();
        if (shapes.isEmpty()) {
            return List.of();
        }

        Set<String> excluded = ShapeSwitchConfig.excludedShapeIds();
        String signature = shapes.size() + "|" + excluded;
        if (signature.equals(cachedSignature)) {
            return cached;
        }

        List<Entry> out = new ArrayList<>(shapes.size());
        for (Object shape : shapes) {
            if (shape == null) {
                continue;
            }
            String id = OkmBridge.shapeId(shape);
            if (id.isBlank() || excluded.contains(id)) {
                continue;
            }
            String key = OkmBridge.shapeTranslationKey(shape);
            if (key.isBlank()) {
                key = "shape." + id.replace(':', '.');
            }
            out.add(new Entry(id, key));
        }

        List<Entry> result = List.copyOf(out);
        cachedSignature = signature;
        cached = result;
        return result;
    }

    /** 当前选中的形状 ID（读 OKM 配置）。 */
    public static String selectedShapeId() {
        return OkmBridge.selectedShape();
    }

    /** 当前选中形状在列表中的下标；找不到时返回 -1。 */
    public static int indexOfSelected(List<Entry> entries) {
        if (entries.isEmpty()) {
            return -1;
        }
        String selected = selectedShapeId();
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).id().equals(selected)) {
                return i;
            }
        }
        return -1;
    }

    /** 把 {@code onekeyminer:small_tunnel} 变成 {@code Small Tunnel}。 */
    public static String humanize(String id) {
        int colon = id.indexOf(':');
        String path = colon >= 0 ? id.substring(colon + 1) : id;
        String[] parts = path.split("[_\\-]+");
        StringBuilder sb = new StringBuilder(path.length());
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                sb.append(part.substring(1));
            }
        }
        return sb.isEmpty() ? id : sb.toString();
    }
}
