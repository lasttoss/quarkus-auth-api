package io.zw.auth.api.models;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.zw.auth.api.constants.Constants;
import jakarta.persistence.*;
import lombok.Data;
import org.joda.time.DateTime;

import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
public class UserModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "social_id", nullable = false, columnDefinition = "varchar(255)")
    private String socialId;

    @Column(name = "social_type", nullable = false, columnDefinition = "int8")
    private int type;

    @Column(name = "display_name", nullable = false, columnDefinition = "varchar(255)")
    private String displayName;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamp default current_timestamp")
    private Date createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamp default current_timestamp")
    private Date updatedAt;

    @Column(name = "last_login_time", nullable = false, columnDefinition = "timestamp default current_timestamp")
    private Date lastLoginTime;

    public UserModel() {
    }

    public UserModel(String username, String password) {
        String uuid = UUID.randomUUID().toString();
        this.userId = uuid;
        this.username = username;
        this.password = BcryptUtil.bcryptHash(password);
        this.displayName = "";
        this.socialId = uuid;
        this.type = Constants.SocialTypeLogin.AUTH_LOGIN.getValue();
        this.createdAt = DateTime.now().toDate();
        this.updatedAt = DateTime.now().toDate();
        this.lastLoginTime = DateTime.now().toDate();
    }
}
