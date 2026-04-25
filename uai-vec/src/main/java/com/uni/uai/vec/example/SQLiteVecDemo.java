package com.uni.uai.vec.example;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.*;

public class SQLiteVecDemo {

    public static void main(String[] args) {
    	// 获取项目根目录的绝对路径
        String vecPath = "/Users/pengjian/work/workspace-uni/uai/uai-vec/vec0"; // 自动匹配 .dylib/.so/.dll
        // SQLite 数据库文件，会自动创建
        String url = "jdbc:sqlite:vec_demo.db?enable_load_extension=true";

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {

            // ======================
            // 1. 加载 sqlite-vec 扩展
            // ======================
            try {
                stmt.execute("SELECT load_extension('vec0')");
                System.out.println("✅ sqlite-vec 加载成功");
            } catch (SQLException e) {
                System.err.println("❌ 加载失败，请检查 vec0 扩展文件是否在项目根目录");
                e.printStackTrace();
                return;
            }

            // ======================
            // 2. 创建向量表（4维向量）
            // ======================
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS items (" +
                "    id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "    vec FLOAT[4] " +  // 4维向量，可改成 384/768/1536
                ")"
            );

            // ======================
            // 3. 插入测试向量
            // ======================
            float[] vec1 = {1.0f, 0.0f, 0.0f, 0.0f};
            float[] vec2 = {0.9f, 0.1f, 0.0f, 0.0f};
            float[] vec3 = {0.0f, 1.0f, 0.0f, 0.0f};

            insertVector(conn, vec1);
            insertVector(conn, vec2);
            insertVector(conn, vec3);
            System.out.println("✅ 插入 3 条向量完成");

            // ======================
            // 4. KNN 相似搜索（最关键）
            // ======================
            float[] queryVec = {1.0f, 0.0f, 0.0f, 0.0f}; // 要搜索的向量
            searchSimilar(conn, queryVec, 3);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 插入向量
    private static void insertVector(Connection conn, float[] vec) throws SQLException {
        String sql = "INSERT INTO items (vec) VALUES (?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBytes(1, floatToBlob(vec));
            pstmt.executeUpdate();
        }
    }

    // 相似向量搜索
    private static void searchSimilar(Connection conn, float[] queryVec, int topK) throws SQLException {
        String sql = """
            SELECT id, vec_distance_L2(vec, ?) AS distance
            FROM items
            ORDER BY distance ASC
            LIMIT ?
        """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBytes(1, floatToBlob(queryVec));
            pstmt.setInt(2, topK);

            ResultSet rs = pstmt.executeQuery();
            System.out.println("\n🔍 相似搜索结果：");
            while (rs.next()) {
                int id = rs.getInt("id");
                double dist = rs.getDouble("distance");
                System.out.printf("id: %d | 距离: %.6f%n", id, dist);
            }
        }
    }

    // float[] 转 BLOB（必须小端序，否则向量错乱）
    private static byte[] floatToBlob(float[] vec) {
        ByteBuffer buffer = ByteBuffer.allocate(vec.length * 4);
        buffer.order(ByteOrder.LITTLE_ENDIAN); // 必须
        for (float v : vec) buffer.putFloat(v);
        return buffer.array();
    }
}
