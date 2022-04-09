package org.railwaystations.rsapi.adapter.out.db;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
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
    Optional<User> findByNormalizedName(@Bind("normalizedName") final String normalizedName);

    @SqlQuery("SELECT * FROM users WHERE id = :id")
    @RegisterRowMapper(UserMapper.class)
    Optional<User> findById(@Bind("id") final int id);

    @SqlQuery("SELECT * FROM users WHERE email = :email")
    @RegisterRowMapper(UserMapper.class)
    Optional<User> findByEmail(@Bind("email") final String email);

    @SqlUpdate("UPDATE users SET `key` = :key WHERE id = :id")
    void updateCredentials(@Bind("id") final int id, @Bind("key") final String key);

    @SqlUpdate("""
            INSERT INTO users (id, name, url, license, email, normalizedName, ownPhotos, anonymous, `key`, emailVerification, sendNotifications)
                VALUES (:id, :name, :url, :license, :email, :normalizedName, :ownPhotos, :anonymous, :key, :emailVerification, :sendNotifications)
            """)
    @GetGeneratedKeys("id")
    Integer insert(@BindBean final User user);

    @SqlUpdate("""
            UPDATE users SET name = :name, url = :url, license = :license, email = :email, normalizedName = :normalizedName, ownPhotos = :ownPhotos,
                            anonymous = :anonymous, emailVerification = :emailVerification, sendNotifications = :sendNotifications
            WHERE id = :id
            """)
    void update(@BindBean final User user);

    @SqlQuery("SELECT * FROM users WHERE emailVerification = :emailVerification")
    @RegisterRowMapper(UserMapper.class)
    Optional<User> findByEmailVerification(@Bind("emailVerification") final String emailVerification);

    @SqlUpdate("UPDATE users SET emailVerification = :emailVerification WHERE id = :id")
    void updateEmailVerification(@Bind("id") final int id, @Bind("emailVerification") final String emailVerification);

    class UserMapper implements RowMapper<User> {
        public User map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            return new User(rs.getString("name"),
                    rs.getString("url"),
                    rs.getString("license"),
                    rs.getInt("id"),
                    rs.getString("email"),
                    rs.getBoolean("ownPhotos"),
                    rs.getBoolean("anonymous"),
                    rs.getString("key"),
                    rs.getBoolean("admin"),
                    rs.getString("emailVerification"),
                    rs.getBoolean("sendNotifications")
                    );
        }
    }

}
