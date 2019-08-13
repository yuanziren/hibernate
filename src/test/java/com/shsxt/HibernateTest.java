package com.shsxt;

import com.shsxt.pojo.User;
import com.shsxt.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.Test;

public class HibernateTest {
    @Test
    public void testInit() {
        SessionFactory sessionFactory = null;
        Session session = null;
        Transaction transaction = null;
        try {
        // 创建服务注册对象
        StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().configure().build();
        // 通过Metadata创建SessionFactory
        sessionFactory = new MetadataSources(serviceRegistry).buildMetadata().buildSessionFactory();
        // 通过SessionFactory得到Session
        session = sessionFactory.openSession();
        // 通过Session对象得到Transaction对象--开启事务
        transaction = session.beginTransaction();



        // 保存数据
        User user = new User();
        user.setUsername("张三");
        user.setPassword("111");

        /***
         * 核心
         * */
        session.save(user);


        // 提交事务
        transaction.commit();
        } catch (Exception e) {
            e.printStackTrace();
        // 回滚事务
            transaction.rollback();
        } finally {
        // 关闭session
            if (session != null && session.isOpen())
                session.close();
        }



    }





    // get/load -> clear/evict
    @Test
    public void testSave() {
        Session session = null;
        Transaction tx = null;
        User user = null;
        try {
            session = HibernateUtil.getSession();
            tx = session.beginTransaction();
            // 构造对象 -> 瞬时状态，session中没有，数据库中没有
            user = new User();
            user.setUsername("刘德华");
            user.setPassword("123");

            // 调用save() -> 持久状态，user被session管理，session中有，数据库中有
            session.save(user);
        /*
         * 在持久状态下,脏数据检查:当提交事务时或清理缓存时,发现session中的数据和
         * 数据库中的数据不一致时,将会把session中的数据更新到数据库中
         */
            user.setUsername("张学友");
            // 在保存以后再修改对象将会产生多条sql语句,效率较低,建议在save前修改
            session.flush();
            tx.commit();
        } catch (Exception e) {
            e.printStackTrace();
            tx.rollback();
        } finally {
            HibernateUtil.closeSession();
        }

//         session被关闭 -> 游离状态，session中没有，数据库中有
        System.out.println("姓名:" + user.getUsername());
        user.setUsername("梁朝伟");

        try {
            session = HibernateUtil.getSession();
            tx = session.beginTransaction();
            // 调用update() -> 持久状态，user被session管理，session中有，数据库中有
            // 如果此时先get()|load()获取到user -> 持久状态，session中有，数据库中有
            // 再调用delete() -> 瞬时状态，sesison中没有，数据库中没有
            session.update(user);
            tx.commit();
        } catch (Exception e) {
            e.printStackTrace();
            tx.rollback();
        } finally {
            HibernateUtil.closeSession();
        }
        // 游离状态
    }

    /****
     * get load
     * */

    @Test
    public void testGet1() {
        Session session = null;
        Transaction tr = null;
        User user = null;
        try {
            session = HibernateUtil.getSession();
            tr = session.beginTransaction();
            // get() -> 持久状态，user被session管理，session中有，数据库中有
            // get()会立即查询该对象：范围从session，SessionFactory，数据库
            user = (User) session.get(User.class, 1);
            System.out.println("姓名:" + user.getUsername());
            tr.commit();
            // clear()清除session缓存中所有对象，evict()清除指定对象
            session.clear();
//            session.evict(user);
            // clear()|evict() -> 游离状态，不被session管理，数据库中不会被更改
            user.setUsername("张国荣");
            System.out.println(user.getUsername());
        } catch (Exception e) {
            e.printStackTrace();
            tr.rollback();
        } finally {
            HibernateUtil.closeSession();
        }
    }

    //get/load的区别：
    //get会立即去查询对象，load在使用才去查询（懒加载），get找不到对象时返回null
    //load找不到对象时抛异常。
    @Test
    public void testGet2() {
        Session session = null;
        Transaction tr = null;
        User user = null;
        try {
            session = HibernateUtil.getSession();
            tr = session.beginTransaction();
            // get() -> 持久状态，user被session管理，session中有，数据库中有
            // get()会立即查询该对象：范围从session，SessionFactory，数据库
            // get()如果找不到对象不会抛异常，返回null
            user = (User) session.get(User.class, 10);
            System.out.println(user);
            System.out.println("姓名:" + user.getUsername());
            tr.commit();
        } catch (Exception e) {
            e.printStackTrace();
            tr.rollback();
        } finally {
            HibernateUtil.closeSession();
        }
    }

    @Test
    public void testLoad() {
        Session session = null;
        Transaction tr = null;
        User user = null;
        try {
            session = HibernateUtil.getSession();
            tr = session.beginTransaction();
            // load() -> 持久状态
            // load()不会立即去查询对象，到使用时才会查询(懒加载)：范围从session，SessionFactory，数据库
            // load()当对象不存在时会抛出org.hibernate.ObjectNotFoundException异常
            user = (User) session.load(User.class, 10);
            System.out.println("姓名:" + user.getUsername());
            tr.commit();
        } catch (Exception e) {
            e.printStackTrace();
            tr.rollback();
        } finally {
            HibernateUtil.closeSession();
        }
    }


    //    update
//    注意：先获取对象进行判断再更新，可以避免异常，提高程序的健壮性。
    @Test
    public void testUpdate() {
        Session session = null;
        Transaction tr = null;
        User user = null;
        try {
            session = HibernateUtil.getSession();
            tr = session.beginTransaction();
            // 手动构造的瞬时状态对象也可以修改，但是需要指定所有属性，不建议使用
            //user = new User();
            //user.setId(3);
            //user.setUsername("李四");
            // get() -> 持久状态，user被session管理，session中有，数据库中有
            user = (User) session.get(User.class, 1);
            // 通过从数据库中加载该对象然后再修改可以进行判断进而避免异常，提高程序的健壮性
            if (null != user) {
                user.setUsername("老王");
                // update() -> 持久状态，user被session管理，session中有，数据库中有
                session.update(user);
            }
            tr.commit();
        } catch (Exception e) {
            e.printStackTrace();
            tr.rollback();
        } finally {
            HibernateUtil.closeSession();
        }
    }

    //    saveOrUpdate
    //    如果对象存在,就更新; 如果不存在就添加
    @Test
    public void testSaveOrUpdate() {
        Session session = null;
        Transaction tr = null;
        User user = null;
        try {
            session = HibernateUtil.getSession();
            tr = session.beginTransaction();
            // 手动构造的瞬时状态对象也可以修改，但是需要指定所有属性，不建议使用
            user = new User();
            user.setId(2);
            user.setUsername("李四");

            session.saveOrUpdate(user);

            tr.commit();
        } catch (Exception e) {
            e.printStackTrace();
            tr.rollback();
        } finally {
            HibernateUtil.closeSession();
        }
    }



    //    delete
    @Test
    public void testDelete() {
        Session session = null;
        Transaction tr = null;
        User user = null;
        try {
            session = HibernateUtil.getSession();
            tr = session.beginTransaction();
            // 手动构造的瞬时状态对象，指定主键也是可以删除该对象的，但是不建议这么用
            //user = new User();
            //user.setId(5);
            // get() -> 持久状态，user被session管理，session中有，数据库中有
            user = (User) session.get(User.class, 10);
            // 通过从数据库中加载该对象然后删除可以进行判断进而避免异常,提高程序的健壮性
            if (null != user) {
                // delete() -> 瞬时状态，session中没有，数据库中没有
                session.delete(user);
            }
            tr.commit();
        } catch (Exception e) {
            e.printStackTrace();
            tr.rollback();
        } finally {
            HibernateUtil.closeSession();
        }
    }





















}