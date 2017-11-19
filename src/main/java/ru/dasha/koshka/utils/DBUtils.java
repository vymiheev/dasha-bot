package ru.dasha.koshka.utils;

import ru.dasha.koshka.myav.MySqlDbConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Daria on 10.11.2017.
 */
public class DBUtils {

    public static void createUser(long id, boolean isChild, String userName) {
        Connection conn = null;
        try {
            conn = MySqlDbConnection.getConnection();
            if (conn != null) {
                CallableStatement st = conn.prepareCall("{call createUser(?, ?, ?)}");
                st.setLong(1, id);
                st.setBoolean(2, isChild);
                st.setString(3, userName);
                st.execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeConnection(conn);
        }
    }

    public static void setChildAge(long id, int age) {
        Connection conn = null;
        try {
            conn = MySqlDbConnection.getConnection();
            if (conn != null) {
                CallableStatement st = conn.prepareCall("{call setChildAge(?, ?)}");
                st.setLong(1, id);
                st.setInt(2, age);
                st.execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeConnection(conn);
        }
    }

    public static int setCorrectWord(long userId, int activityId, String word) {
        Connection conn = null;
        try {
            conn = MySqlDbConnection.getConnection();
            if (conn != null) {
                CallableStatement st = conn.prepareCall("{? = call setCorrectWord(?, ?, ?)}");
                st.registerOutParameter(1, Types.NUMERIC);
                st.setLong(2, userId);
                st.setInt(3, activityId);
                st.setString(4, word);
                st.execute();
                return st.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeConnection(conn);
        }
        return 0;
    }

    public static void setUserAction(long id, int action, Integer activityId) {
        Connection conn = null;
        try {
            conn = MySqlDbConnection.getConnection();
            if (conn != null) {
                String insert = "UPDATE user_action set action_id = ?, activity_id = ? where user_id = ?";
                PreparedStatement st = conn.prepareStatement(insert);
                st.setInt(1, action);
                //st.setInt(2, activityId);
                st.setObject(2, activityId);
                st.setLong(3, id);
                st.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeConnection(conn);
        }
    }


    public static void createUserAction(long id, boolean isChild, String userName) {
        Connection conn = null;
        try {
            conn = MySqlDbConnection.getConnection();
            if (conn != null) {
                String insert = "INSERT INTO users (id, is_child, userName) values (?,?,?)";
                PreparedStatement st = conn.prepareStatement(insert);
                st.setLong(1, id);
                st.setBoolean(2, isChild);
                st.setString(3, userName);
                st.executeQuery();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeConnection(conn);
        }
    }

    public static int getUserAction(long id) {
        Connection conn = null;
        try {
            conn = MySqlDbConnection.getConnection();
            if (conn != null) {
                String select = "SELECT action_id FROM user_action where user_id = ?";
                PreparedStatement st = conn.prepareStatement(select);
                st.setLong(1, id);
                ResultSet rs = st.executeQuery();
                int res = -1;
                while (rs.next()) {
                    res = rs.getInt("action_id");
                }
                return res;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeConnection(conn);
        }
        return -1;
    }

    public static int getUserActionActivityId(long id) {
        Connection conn = null;
        try {
            conn = MySqlDbConnection.getConnection();
            if (conn != null) {
                String select = "SELECT activity_id FROM user_action where user_id = ?";
                PreparedStatement st = conn.prepareStatement(select);
                st.setLong(1, id);
                ResultSet rs = st.executeQuery();
                int res = -1;
                while (rs.next()) {
                    res = rs.getInt("activity_id");
                }
                return res;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeConnection(conn);
        }
        return -1;
    }

    public static int getActivityWordAmount(long id, int activityId) {
        Connection conn = null;
        try {
            conn = MySqlDbConnection.getConnection();
            if (conn != null) {
                String select = "SELECT count(*) t FROM user_words where user_id = ? and activity_id = ?";
                PreparedStatement st = conn.prepareStatement(select);
                st.setLong(1, id);
                st.setInt(2, activityId);
                ResultSet rs = st.executeQuery();
                int res = -1;
                while (rs.next()) {
                    res = rs.getInt("t");
                }
                return res;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeConnection(conn);
        }
        return -1;
    }

    public static int getPoints(long id) {
        Connection conn = null;
        try {
            conn = MySqlDbConnection.getConnection();
            if (conn != null) {
                String select = "SELECT count(*) t FROM user_words where user_id = ?";
                PreparedStatement st = conn.prepareStatement(select);
                st.setLong(1, id);
                ResultSet rs = st.executeQuery();
                int res = -1;
                while (rs.next()) {
                    res = rs.getInt("t");
                }
                return res;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeConnection(conn);
        }
        return -1;
    }

    public static int getAge(long id) {
        Connection conn = null;
        try {
            conn = MySqlDbConnection.getConnection();
            if (conn != null) {
                String select = "SELECT age FROM users where id = ?";
                PreparedStatement st = conn.prepareStatement(select);
                st.setLong(1, id);
                ResultSet rs = st.executeQuery();
                int res = -1;
                while (rs.next()) {
                    res = rs.getInt("age");
                }
                return res;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeConnection(conn);
        }
        return -1;
    }

    public static List<Long> getChatIds() {
        Connection conn = null;
        try {
            conn = MySqlDbConnection.getConnection();
            if (conn != null) {
                String select = "SELECT id FROM users";
                PreparedStatement st = conn.prepareStatement(select);
                ResultSet rs = st.executeQuery();
                List<Long> chatIds = new ArrayList<>();
                while (rs.next()) {
                    chatIds.add(rs.getLong("id"));
                }
                return chatIds;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeConnection(conn);
        }
        return null;
    }

    private static void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
