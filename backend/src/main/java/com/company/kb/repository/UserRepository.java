package com.company.kb.repository;

import com.company.kb.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户数据访问层（User Repository）— 用户实体的数据库操作接口
 *
 * <h2>架构角色</h2>
 * <p>属于数据访问层（Data Access Layer / DAO Layer），遵循 Repository 模式。
 * 负责封装所有与 User 实体相关的数据库操作，使上层 Service 层无需关心 SQL 细节。</p>
 *
 * <h2>Spring Data JPA 的"魔法"</h2>
 * <p>这是一个<b>接口</b>，没有实现类！Spring Data JPA 在运行时会自动生成实现类
 * （基于 JDK 动态代理）。它通过解析方法名来推导 SQL 查询，这就是
 * <b>派生查询（Derived Query / Query Creation from Method Name）</b> 机制。</p>
 *
 * <h3>方法名查询推导规则</h3>
 * <table>
 *   <tr><th>关键字</th><th>SQL 条件</th><th>示例</th></tr>
 *   <tr><td>findBy</td><td>WHERE ...</td><td>{@code findByUsername}</td></tr>
 *   <tr><td>And</td><td>AND</td><td>{@code findByUsernameAndEmail}</td></tr>
 *   <tr><td>Or</td><td>OR</td><td>{@code findByUsernameOrEmail}</td></tr>
 *   <tr><td>existsBy</td><td>SELECT COUNT(*) > 0</td><td>{@code existsByUsername}</td></tr>
 *   <tr><td>Like</td><td>LIKE</td><td>{@code findByNameLike}</td></tr>
 *   <tr><td>OrderBy</td><td>ORDER BY</td><td>{@code findByRoleOrderByName}</td></tr>
 * </table>
 *
 * 💡 学习要点:
 * <ul>
 *   <li><b>Repository 模式</b>: 将数据访问逻辑抽象为接口，隔离业务逻辑和数据库操作。
 *       Service 层只依赖 Repository 接口，不直接操作 EntityManager 或 JDBC。</li>
 *   <li><b>{@code JpaRepository<Entity, ID>}</b>: 继承此接口即可获得 CRUD、分页、排序等
 *       十几种通用方法，如 save()、findById()、findAll()、delete() 等。</li>
 *   <li><b>{@code Optional}</b> 返回值: Spring Data JPA 对单结果查询返回 {@code Optional}，
 *       强制调用者处理"查不到数据"的情况，避免 NullPointerException。</li>
 *   <li><b>{@code boolean} 返回值</b>: 对于 exists 开头的方法，返回 boolean，
 *       Spring Data 内部使用 COUNT 查询，比 find + isPresent 更高效。</li>
 *   <li><b>无需 {@code @Repository} 注解</b>: 继承 JpaRepository 的接口会被 Spring Data
 *       自动扫描注册。但加上 {@code @Repository} 是好习惯，增加可读性。</li>
 * </ul>
 *
 * @author Geekyous Guo
 * @since 1.0.0
 * @see JpaRepository
 * @see User
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 根据用户名查找用户。
     *
     * <p>Spring Data JPA 解析方法名 {@code findByUsername}，自动生成 SQL：
     * {@code SELECT * FROM users WHERE username = ? AND deleted_at IS NULL}</p>
     *
     * <p>使用场景：登录认证时根据用户名查找用户记录。</p>
     *
     * @param username 要查找的用户名（精确匹配）
     * @return {@code Optional<User>} — 如果找到则包含用户对象，否则为空 Optional。
     *         使用 Optional 避免 NullPointerException，推荐调用方式：
     *         {@code userRepository.findByUsername("admin").orElseThrow(...)}
     */
    Optional<User> findByUsername(String username);

    /**
     * 根据邮箱查找用户。
     *
     * <p>自动生成 SQL：{@code SELECT * FROM users WHERE email = ?}</p>
     *
     * <p>使用场景：通过邮箱找回密码、检测邮箱是否已注册。</p>
     *
     * @param email 要查找的邮箱地址（精确匹配）
     * @return 包含匹配用户的 Optional，可能为空
     */
    Optional<User> findByEmail(String email);

    /**
     * 检查用户名是否已存在。
     *
     * <p>Spring Data JPA 解析方法名 {@code existsByUsername}，自动生成 SQL：
     * {@code SELECT COUNT(*) > 0 FROM users WHERE username = ?}</p>
     *
     * <p>比 {@code findByUsername().isPresent()} 更高效，因为不需要加载完整实体，
     * 只返回 true/false。使用场景：注册时校验用户名唯一性。</p>
     *
     * @param username 要检查的用户名
     * @return true 表示用户名已被占用，false 表示可用
     */
    boolean existsByUsername(String username);

    /**
     * 检查邮箱是否已注册。
     *
     * <p>自动生成 SQL：{@code SELECT COUNT(*) > 0 FROM users WHERE email = ?}</p>
     *
     * <p>使用场景：注册时校验邮箱唯一性。</p>
     *
     * @param email 要检查的邮箱地址
     * @return true 表示邮箱已被注册，false 表示可用
     */
    boolean existsByEmail(String email);
}
