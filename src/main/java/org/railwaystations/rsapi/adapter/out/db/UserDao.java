package org.railwaystations.rsapi.adapter.out.db;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.railwaystations.rsapi.core.model.License;
import org.railwaystations.rsapi.core.model.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface UserDao {

    @SqlQuery("SELECT * FROM users")
    @RegisterRowMapper(UserMapper.class)
    List<User> list();

    @SqlQuery("SELECT * FROM users WHERE normalizedName = :normalizedName")
    @RegisterRowMapper(UserMapper.class)
    Optional<User> findByNormalizedName(@Bind("normalizedName") String normalizedName);

    @SqlQuery("SELECT * FROM users WHERE id = :id")
    @RegisterRowMapper(UserMapper.class)
    Optional<User> findById(@Bind("id") int id);

    @SqlQuery("SELECT * FROM users WHERE email = :email")
    @RegisterRowMapper(UserMapper.class)
    Optional<User> findByEmail(@Bind("email") String email);

    @SqlUpdate("UPDATE users SET `key` = :key WHERE id = :id")
    void updateCredentials(@Bind("id") int id, @Bind("key") String key);

    @SqlUpdate("""
            INSERT INTO users (id, name, url, license, email, normalizedName, ownPhotos, anonymous, `key`, emailVerification, sendNotifications)
                VALUES (:id, :name, :url, :license, :email, :normalizedName, :ownPhotos, :anonymous, :key, :emailVerification, :sendNotifications)
            """)
    @GetGeneratedKeys("id")
    Integer insert(@BindBean User user, @Bind("key") String key, @Bind("emailVerification") String emailVerification);

    @SqlUpdate("""
            UPDATE users SET name = :name, url = :url, license = :license, email = :email, normalizedName = :normalizedName, ownPhotos = :ownPhotos,
                            anonymous = :anonymous, sendNotifications = :sendNotifications
            WHERE id = :id
            """)
    void update(@Bind("id") int id, @BindBean User user);

    @SqlQuery("SELECT * FROM users WHERE emailVerification = :emailVerification")
    @RegisterRowMapper(UserMapper.class)
    Optional<User> findByEmailVerification(@Bind("emailVerification") String emailVerification);

    @SqlUpdate("UPDATE users SET emailVerification = :emailVerification WHERE id = :id")
    void updateEmailVerification(@Bind("id") int id, @Bind("emailVerification") String emailVerification);

    class UserMapper implements RowMapper<User> {
        public User map(ResultSet rs, StatementContext ctx) throws SQLException {
            return User.builder()
                    .id(rs.getInt("id"))
                    .name(rs.getString("name"))
                    .url(rs.getString("url"))
                    .license(License.valueOf(rs.getString("license")))
                    .email(rs.getString("email"))
                    .ownPhotos(rs.getBoolean("ownPhotos"))
                    .anonymous(rs.getBoolean("anonymous"))
                    .key(rs.getString("key"))
                    .admin(rs.getBoolean("admin"))
                    .emailVerification(rs.getString("emailVerification"))
                    .sendNotifications(rs.getBoolean("sendNotifications"))
                    .build();
        }
    }

}
