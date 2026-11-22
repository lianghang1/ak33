package com.sfi.matrix.cemetery.strategy;

/**
 * 默认编号策略：区-排-号。
 *
 * <p>编号格式：{区码}-{排号2位}-{位号2位}，如 A-01-01（不足补零，排/位从1开始）；
 * 基础路径：{园}/{区}/{排}排；全路径：基础路径/{位}号。多级墓区下区码取叶子墓区、
 * 园名取根节点，由 Service 组装上下文时保证。</p>
 *
 * @author matrix
 */
public class ZoneRowNoStrategy implements TombCodeStrategy {

    @Override
    public String name() {
        return "ZoneRowNo";
    }

    @Override
    public CodeResult generate(CodeContext ctx) {
        String zone = nvl(ctx.getZoneCode());
        String code = zone + "-" + pad(ctx.getRowNo(), 2) + "-" + pad(ctx.getColNo(), 2);

        String garden = nvl(ctx.getGardenName());
        String zoneName = nvl(ctx.getZoneName());
        String basePath = join(garden, zoneName, ctx.getRowNo() + "排");
        String fullPath = basePath + "/" + pad(ctx.getColNo(), 2) + "号";

        return new CodeResult(code, basePath, fullPath);
    }

    private static String pad(int n, int len) {
        return String.format("%0" + len + "d", n);
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }

    private static String join(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p == null || p.isEmpty()) continue;
            if (sb.length() > 0) sb.append('/');
            sb.append(p);
        }
        return sb.toString();
    }
}
