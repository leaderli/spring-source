/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core;

import org.junit.Test;
import org.springframework.util.ReflectionUtils;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Chris Beams
 */
@SuppressWarnings("rawtypes")
public class BridgeMethodResolverTests {

    private static Method findMethodWithReturnType(String name, Class<?> returnType, Class<SettingsDaoImpl> targetType) {
        Method[] methods = targetType.getMethods();
        for (Method m : methods) {
            if (m.getName().equals(name) && m.getReturnType().equals(returnType)) {
                return m;
            }
        }
        return null;
    }


    @Test
    public void testFindBridgedMethod() throws Exception {
        Method unbridged = MyFoo.class.getDeclaredMethod("someMethod", String.class, Object.class);
        Method bridged = MyFoo.class.getDeclaredMethod("someMethod", Serializable.class, Object.class);
        assertThat(unbridged.isBridge()).isFalse();
        assertThat(bridged.isBridge()).isTrue();

        assertThat(BridgeMethodResolver.findBridgedMethod(unbridged)).as("Unbridged method not returned directly").isEqualTo(unbridged);
        assertThat(BridgeMethodResolver.findBridgedMethod(bridged)).as("Incorrect bridged method returned").isEqualTo(unbridged);
    }

    @Test
    public void testFindBridgedVarargMethod() throws Exception {
        Method unbridged = MyFoo.class.getDeclaredMethod("someVarargMethod", String.class, Object[].class);
        Method bridged = MyFoo.class.getDeclaredMethod("someVarargMethod", Serializable.class, Object[].class);
        assertThat(unbridged.isBridge()).isFalse();
        assertThat(bridged.isBridge()).isTrue();

        assertThat(BridgeMethodResolver.findBridgedMethod(unbridged)).as("Unbridged method not returned directly").isEqualTo(unbridged);
        assertThat(BridgeMethodResolver.findBridgedMethod(bridged)).as("Incorrect bridged method returned").isEqualTo(unbridged);
    }

    @Test
    public void testFindBridgedMethodInHierarchy() throws Exception {
        Method bridgeMethod = DateAdder.class.getMethod("add", Object.class);
        assertThat(bridgeMethod.isBridge()).isTrue();
        Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(bridgeMethod);
        assertThat(bridgedMethod.isBridge()).isFalse();
        assertThat(bridgedMethod.getName()).isEqualTo("add");
        assertThat(bridgedMethod.getParameterCount()).isEqualTo(1);
        assertThat(bridgedMethod.getParameterTypes()[0]).isEqualTo(Date.class);
    }

    @Test
    public void testIsBridgeMethodFor() throws Exception {
        Method bridged = MyBar.class.getDeclaredMethod("someMethod", String.class, Object.class);
        Method other = MyBar.class.getDeclaredMethod("someMethod", Integer.class, Object.class);
        Method bridge = MyBar.class.getDeclaredMethod("someMethod", Object.class, Object.class);

        assertThat(BridgeMethodResolver.isBridgeMethodFor(bridge, bridged, MyBar.class)).as("Should be bridge method").isTrue();
        assertThat(BridgeMethodResolver.isBridgeMethodFor(bridge, other, MyBar.class)).as("Should not be bridge method").isFalse();
    }

    @Test
    public void testDoubleParameterization() throws Exception {
        Method objectBridge = MyBoo.class.getDeclaredMethod("foo", Object.class);
        Method serializableBridge = MyBoo.class.getDeclaredMethod("foo", Serializable.class);

        Method stringFoo = MyBoo.class.getDeclaredMethod("foo", String.class);
        Method integerFoo = MyBoo.class.getDeclaredMethod("foo", Integer.class);

        assertThat(BridgeMethodResolver.findBridgedMethod(objectBridge)).as("foo(String) not resolved.").isEqualTo(stringFoo);
        assertThat(BridgeMethodResolver.findBridgedMethod(serializableBridge)).as("foo(Integer) not resolved.").isEqualTo(integerFoo);
    }

    @Test
    public void testFindBridgedMethodFromMultipleBridges() throws Exception {
        Method loadWithObjectReturn = findMethodWithReturnType("load", Object.class, SettingsDaoImpl.class);
        assertThat(loadWithObjectReturn).isNotNull();

        Method loadWithSettingsReturn = findMethodWithReturnType("load", Settings.class, SettingsDaoImpl.class);
        assertThat(loadWithSettingsReturn).isNotNull();
        assertThat(loadWithSettingsReturn).isNotSameAs(loadWithObjectReturn);

        Method method = SettingsDaoImpl.class.getMethod("load");
        assertThat(BridgeMethodResolver.findBridgedMethod(loadWithObjectReturn)).isEqualTo(method);
        assertThat(BridgeMethodResolver.findBridgedMethod(loadWithSettingsReturn)).isEqualTo(method);
    }

    @Test
    public void testFindBridgedMethodFromParent() throws Exception {
        Method loadFromParentBridge = SettingsDaoImpl.class.getMethod("loadFromParent");
        assertThat(loadFromParentBridge.isBridge()).isTrue();

        Method loadFromParent = AbstractDaoImpl.class.getMethod("loadFromParent");
        assertThat(loadFromParent.isBridge()).isFalse();

        assertThat(BridgeMethodResolver.findBridgedMethod(loadFromParentBridge)).isEqualTo(loadFromParent);
    }

    @Test
    public void testWithSingleBoundParameterizedOnInstantiate() throws Exception {
        Method bridgeMethod = DelayQueue.class.getMethod("add", Object.class);
        assertThat(bridgeMethod.isBridge()).isTrue();
        Method actualMethod = DelayQueue.class.getMethod("add", Delayed.class);
        assertThat(actualMethod.isBridge()).isFalse();
        assertThat(BridgeMethodResolver.findBridgedMethod(bridgeMethod)).isEqualTo(actualMethod);
    }

    @Test
    public void testWithDoubleBoundParameterizedOnInstantiate() throws Exception {
        Method bridgeMethod = SerializableBounded.class.getMethod("boundedOperation", Object.class);
        assertThat(bridgeMethod.isBridge()).isTrue();
        Method actualMethod = SerializableBounded.class.getMethod("boundedOperation", HashMap.class);
        assertThat(actualMethod.isBridge()).isFalse();
        assertThat(BridgeMethodResolver.findBridgedMethod(bridgeMethod)).isEqualTo(actualMethod);
    }

    @Test
    public void testWithGenericParameter() throws Exception {
        Method[] methods = StringGenericParameter.class.getMethods();
        Method bridgeMethod = null;
        Method bridgedMethod = null;
        for (Method method : methods) {
            if ("getFor".equals(method.getName()) && !method.getParameterTypes()[0].equals(Integer.class)) {
                if (method.getReturnType().equals(Object.class)) {
                    bridgeMethod = method;
                } else {
                    bridgedMethod = method;
                }
            }
        }
        assertThat(bridgeMethod != null && bridgeMethod.isBridge()).isTrue();
        boolean condition = bridgedMethod != null && !bridgedMethod.isBridge();
        assertThat(condition).isTrue();
        assertThat(BridgeMethodResolver.findBridgedMethod(bridgeMethod)).isEqualTo(bridgedMethod);
    }

    @Test
    public void testOnAllMethods() throws Exception {
        Method[] methods = StringList.class.getMethods();
        for (Method method : methods) {
            assertThat(BridgeMethodResolver.findBridgedMethod(method)).isNotNull();
        }
    }

    @Test
    public void testSPR2583() throws Exception {
        Method bridgedMethod = MessageBroadcasterImpl.class.getMethod("receive", MessageEvent.class);
        assertThat(bridgedMethod.isBridge()).isFalse();
        Method bridgeMethod = MessageBroadcasterImpl.class.getMethod("receive", Event.class);
        assertThat(bridgeMethod.isBridge()).isTrue();

        Method otherMethod = MessageBroadcasterImpl.class.getMethod("receive", NewMessageEvent.class);
        assertThat(otherMethod.isBridge()).isFalse();

        assertThat(BridgeMethodResolver.isBridgeMethodFor(bridgeMethod, otherMethod, MessageBroadcasterImpl.class)).as("Match identified incorrectly").isFalse();
        assertThat(BridgeMethodResolver.isBridgeMethodFor(bridgeMethod, bridgedMethod, MessageBroadcasterImpl.class)).as("Match not found correctly").isTrue();

        assertThat(BridgeMethodResolver.findBridgedMethod(bridgeMethod)).isEqualTo(bridgedMethod);
    }

    @Test
    public void testSPR2603() throws Exception {
        Method objectBridge = YourHomer.class.getDeclaredMethod("foo", Bounded.class);
        Method abstractBoundedFoo = YourHomer.class.getDeclaredMethod("foo", AbstractBounded.class);

        Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(objectBridge);
        assertThat(bridgedMethod).as("foo(AbstractBounded) not resolved.").isEqualTo(abstractBoundedFoo);
    }

    @Test
    public void testSPR2648() throws Exception {
        Method bridgeMethod = ReflectionUtils.findMethod(GenericSqlMapIntegerDao.class, "saveOrUpdate", Object.class);
        assertThat(bridgeMethod != null && bridgeMethod.isBridge()).isTrue();
        Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(bridgeMethod);
        assertThat(bridgedMethod.isBridge()).isFalse();
        assertThat(bridgedMethod.getName()).isEqualTo("saveOrUpdate");
    }

    @Test
    public void testSPR2763() throws Exception {
        Method bridgedMethod = AbstractDao.class.getDeclaredMethod("save", Object.class);
        assertThat(bridgedMethod.isBridge()).isFalse();

        Method bridgeMethod = UserDaoImpl.class.getDeclaredMethod("save", User.class);
        assertThat(bridgeMethod.isBridge()).isTrue();

        assertThat(BridgeMethodResolver.findBridgedMethod(bridgeMethod)).isEqualTo(bridgedMethod);
    }

    @Test
    public void testSPR3041() throws Exception {
        Method bridgedMethod = BusinessDao.class.getDeclaredMethod("save", Business.class);
        assertThat(bridgedMethod.isBridge()).isFalse();

        Method bridgeMethod = BusinessDao.class.getDeclaredMethod("save", Object.class);
        assertThat(bridgeMethod.isBridge()).isTrue();

        assertThat(BridgeMethodResolver.findBridgedMethod(bridgeMethod)).isEqualTo(bridgedMethod);
    }

    @Test
    public void testSPR3173() throws Exception {
        Method bridgedMethod = UserDaoImpl.class.getDeclaredMethod("saveVararg", User.class, Object[].class);
        assertThat(bridgedMethod.isBridge()).isFalse();

        Method bridgeMethod = UserDaoImpl.class.getDeclaredMethod("saveVararg", Object.class, Object[].class);
        assertThat(bridgeMethod.isBridge()).isTrue();

        assertThat(BridgeMethodResolver.findBridgedMethod(bridgeMethod)).isEqualTo(bridgedMethod);
    }

    @Test
    public void testSPR3304() throws Exception {
        Method bridgedMethod = MegaMessageProducerImpl.class.getDeclaredMethod("receive", MegaMessageEvent.class);
        assertThat(bridgedMethod.isBridge()).isFalse();

        Method bridgeMethod = MegaMessageProducerImpl.class.getDeclaredMethod("receive", MegaEvent.class);
        assertThat(bridgeMethod.isBridge()).isTrue();

        assertThat(BridgeMethodResolver.findBridgedMethod(bridgeMethod)).isEqualTo(bridgedMethod);
    }

    @Test
    public void testSPR3324() throws Exception {
        Method bridgedMethod = BusinessDao.class.getDeclaredMethod("get", Long.class);
        assertThat(bridgedMethod.isBridge()).isFalse();

        Method bridgeMethod = BusinessDao.class.getDeclaredMethod("get", Object.class);
        assertThat(bridgeMethod.isBridge()).isTrue();

        assertThat(BridgeMethodResolver.findBridgedMethod(bridgeMethod)).isEqualTo(bridgedMethod);
    }

    @Test
    public void testSPR3357() throws Exception {
        Method bridgedMethod = ExtendsAbstractImplementsInterface.class.getDeclaredMethod(
                "doSomething", DomainObjectExtendsSuper.class, Object.class);
        assertThat(bridgedMethod.isBridge()).isFalse();

        Method bridgeMethod = ExtendsAbstractImplementsInterface.class.getDeclaredMethod(
                "doSomething", DomainObjectSuper.class, Object.class);
        assertThat(bridgeMethod.isBridge()).isTrue();

        assertThat(BridgeMethodResolver.findBridgedMethod(bridgeMethod)).isEqualTo(bridgedMethod);
    }

    @Test
    public void testSPR3485() throws Exception {
        Method bridgedMethod = DomainObject.class.getDeclaredMethod(
                "method2", ParameterType.class, byte[].class);
        assertThat(bridgedMethod.isBridge()).isFalse();

        Method bridgeMethod = DomainObject.class.getDeclaredMethod(
                "method2", Serializable.class, Object.class);
        assertThat(bridgeMethod.isBridge()).isTrue();

        assertThat(BridgeMethodResolver.findBridgedMethod(bridgeMethod)).isEqualTo(bridgedMethod);
    }

    @Test
    public void testSPR3534() throws Exception {
        Method bridgeMethod = ReflectionUtils.findMethod(TestEmailProvider.class, "findBy", Object.class);
        assertThat(bridgeMethod != null && bridgeMethod.isBridge()).isTrue();
        Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(bridgeMethod);
        assertThat(bridgedMethod.isBridge()).isFalse();
        assertThat(bridgedMethod.getName()).isEqualTo("findBy");
    }

    @Test  // SPR-16103
    public void testClassHierarchy() throws Exception {
        doTestHierarchyResolution(FooClass.class);
    }

    @Test  // SPR-16103
    public void testInterfaceHierarchy() throws Exception {
        doTestHierarchyResolution(FooInterface.class);
    }

    private void doTestHierarchyResolution(Class<?> clazz) throws Exception {
        for (Method method : clazz.getDeclaredMethods()) {
            Method bridged = BridgeMethodResolver.findBridgedMethod(method);
            Method expected = clazz.getMethod("test", FooEntity.class);
            assertThat(bridged).isEqualTo(expected);
        }
    }


    public interface Foo<T extends Serializable> {

        void someMethod(T theArg, Object otherArg);

        void someVarargMethod(T theArg, Object... otherArg);
    }


    public interface Adder<T> {

        void add(T item);
    }


    public interface Boo<E, T extends Serializable> {

        void foo(E e);

        void foo(T t);
    }


    public interface Settings {
    }


    public interface ConcreteSettings extends Settings {
    }


    public interface Dao<T, S> {

        T load();

        S loadFromParent();
    }


    public interface SettingsDao<T extends Settings, S> extends Dao<T, S> {

        @Override
        T load();
    }


    public interface ConcreteSettingsDao extends SettingsDao<ConcreteSettings, String> {

        @Override
        String loadFromParent();
    }


    public interface Bounded<E> {

        boolean boundedOperation(E e);
    }


    public interface GenericParameter<T> {

        T getFor(Class<T> cls);
    }


    public interface Event {

        int getPriority();
    }


    public interface UserInitiatedEvent {
    }


    public interface Channel<E extends Event> {

        void send(E event);

        void subscribe(final Receiver<E> receiver, Class<E> event);

        void unsubscribe(final Receiver<E> receiver, Class<E> event);
    }


    public interface Broadcaster {
    }


    public interface EventBroadcaster extends Broadcaster {

        void subscribe();

        void unsubscribe();

        void setChannel(Channel<?> channel);
    }


    public interface Receiver<E extends Event> {

        void receive(E event);
    }


    public interface MessageBroadcaster extends Receiver<MessageEvent> {

    }


    public interface SimpleGenericRepository<T> {

        public Class<T> getPersistentClass();

        List<T> findByQuery();

        List<T> findAll();

        T refresh(T entity);

        T saveOrUpdate(T entity);

        void delete(Collection<T> entities);
    }


    public interface RepositoryRegistry {

        <T> SimpleGenericRepository<T> getFor(Class<T> entityType);
    }


    public interface ConvenientGenericRepository<T, ID extends Serializable>
            extends SimpleGenericRepository<T> {

        T findById(ID id, boolean lock);

        List<T> findByExample(T exampleInstance);

        void delete(ID id);

        void delete(T entity);
    }


    public interface Homer<E> {

        void foo(E e);
    }


    public interface GenericDao<T> {

        void saveOrUpdate(T t);
    }


    public interface ConvenienceGenericDao<T> extends GenericDao<T> {
    }


    public interface UserDao {

        // @Transactional
        void save(User user);

        // @Transactional
        void save(Permission perm);
    }


    public interface DaoInterface<T, P> {

        T get(P id);
    }


    public interface MegaReceiver<E extends MegaEvent> {

        void receive(E event);
    }


    public interface MegaMessageProducer extends MegaReceiver<MegaMessageEvent> {
    }


    public interface IGenericInterface<D extends DomainObjectSuper> {

        <T> void doSomething(final D domainObject, final T value);
    }


    public interface SearchProvider<RETURN_TYPE, CONDITIONS_TYPE> {

        Collection<RETURN_TYPE> findBy(CONDITIONS_TYPE conditions);
    }


    public interface IExternalMessageProvider<S extends ExternalMessage, T extends ExternalMessageSearchConditions<?>>
            extends SearchProvider<S, T> {
    }


    public interface BaseInterface<T> {

        <S extends T> S test(S T);
    }


    public interface EntityInterface<T extends BaseEntity> extends BaseInterface<T> {

        @Override
        <S extends T> S test(S T);
    }


    public interface FooInterface extends EntityInterface<FooEntity> {

        @Override
        <S extends FooEntity> S test(S T);
    }

    public static class MyFoo implements Foo<String> {

        public void someMethod(Integer theArg, Object otherArg) {
        }

        @Override
        public void someMethod(String theArg, Object otherArg) {
        }

        @Override
        public void someVarargMethod(String theArg, Object... otherArgs) {
        }
    }

    public static abstract class Bar<T> {

        void someMethod(Map<?, ?> m, Object otherArg) {
        }

        void someMethod(T theArg, Map<?, ?> m) {
        }

        abstract void someMethod(T theArg, Object otherArg);
    }

    public static abstract class InterBar<T> extends Bar<T> {

    }

    public static class MyBar extends InterBar<String> {

        @Override
        public void someMethod(String theArg, Object otherArg) {
        }

        public void someMethod(Integer theArg, Object otherArg) {
        }
    }

    public static abstract class AbstractDateAdder implements Adder<Date> {

        @Override
        public abstract void add(Date date);
    }

    public static class DateAdder extends AbstractDateAdder {

        @Override
        public void add(Date date) {
        }
    }

    public static class Enclosing<T> {

        public class Enclosed<S> {

            public class ReallyDeepNow<R> {

                void someMethod(S s, T t, R r) {
                }
            }
        }
    }

    public static class ExtendsEnclosing extends Enclosing<String> {

        public class ExtendsEnclosed extends Enclosed<Integer> {

            public class ExtendsReallyDeepNow extends ReallyDeepNow<Long> {

                @Override
                void someMethod(Integer s, String t, Long r) {
                    throw new UnsupportedOperationException();
                }
            }
        }
    }


    //-----------------------------
    // SPR-2454 Test Classes
    //-----------------------------

    public static class MyBoo implements Boo<String, Integer> {

        @Override
        public void foo(String e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void foo(Integer t) {
            throw new UnsupportedOperationException();
        }
    }

    static abstract class AbstractDaoImpl<T, S> implements Dao<T, S> {

        protected T object;

        protected S otherObject;

        protected AbstractDaoImpl(T object, S otherObject) {
            this.object = object;
            this.otherObject = otherObject;
        }

        // @Transactional(readOnly = true)
        @Override
        public S loadFromParent() {
            return otherObject;
        }
    }

    static class SettingsDaoImpl extends AbstractDaoImpl<ConcreteSettings, String>
            implements ConcreteSettingsDao {

        protected SettingsDaoImpl(ConcreteSettings object) {
            super(object, "From Parent");
        }

        // @Transactional(readOnly = true)
        @Override
        public ConcreteSettings load() {
            return super.object;
        }
    }

    private static class AbstractBounded<E> implements Bounded<E> {

        @Override
        public boolean boundedOperation(E myE) {
            return true;
        }
    }

    private static class SerializableBounded<E extends HashMap & Delayed> extends AbstractBounded<E> {

        @Override
        public boolean boundedOperation(E myE) {
            return false;
        }
    }

    @SuppressWarnings("unused")
    private static class StringGenericParameter implements GenericParameter<String> {

        @Override
        public String getFor(Class<String> cls) {
            return "foo";
        }

        public String getFor(Integer integer) {
            return "foo";
        }
    }


    //-------------------
    // SPR-2603 classes
    //-------------------

    private static class StringList implements List<String> {

        @Override
        public int size() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isEmpty() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean contains(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Iterator<String> iterator() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object[] toArray() {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean add(String o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(Collection<? extends String> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(int index, Collection<? extends String> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String get(int index) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String set(int index, String element) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(int index, String element) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String remove(int index) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int indexOf(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int lastIndexOf(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ListIterator<String> listIterator() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ListIterator<String> listIterator(int index) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<String> subList(int fromIndex, int toIndex) {
            throw new UnsupportedOperationException();
        }
    }

    public static class GenericEvent implements Event {

        private int priority;

        /**
         * Constructor that takes an event priority
         */
        public GenericEvent(int priority) {
            this.priority = priority;
        }

        /**
         * Default Constructor
         */
        public GenericEvent() {
        }

        @Override
        public int getPriority() {
            return priority;
        }
    }

    public static abstract class BaseUserInitiatedEvent extends GenericEvent implements UserInitiatedEvent {
    }

    public static class MessageEvent extends BaseUserInitiatedEvent {
    }

    public static class GenericBroadcasterImpl implements Broadcaster {
    }

    @SuppressWarnings({"unused", "unchecked"})
    public static abstract class GenericEventBroadcasterImpl<T extends Event>
            extends GenericBroadcasterImpl implements EventBroadcaster {

        private Class<T>[] subscribingEvents;

        private Channel<T> channel;
        private String beanName;

        public GenericEventBroadcasterImpl(Class<? extends T>... events) {
        }

        /**
         * Abstract method to retrieve instance of subclass
         *
         * @return receiver instance
         */
        public abstract Receiver<T> getInstance();

        @Override
        public void setChannel(Channel channel) {
            this.channel = channel;
        }

        public void setBeanName(String name) {
            this.beanName = name;
        }

        @Override
        public void subscribe() {
        }

        @Override
        public void unsubscribe() {
        }
    }

    public static class RemovedMessageEvent extends MessageEvent {

    }

    public static class NewMessageEvent extends MessageEvent {

    }

    public static class ModifiedMessageEvent extends MessageEvent {

    }

    @SuppressWarnings({"serial", "unchecked"})
    public static class MessageBroadcasterImpl extends GenericEventBroadcasterImpl<MessageEvent>
            implements Serializable,  // implement an unrelated interface first (SPR-16288)
            MessageBroadcaster {

        public MessageBroadcasterImpl() {
            super(NewMessageEvent.class);
        }

        @Override
        public void receive(MessageEvent event) {
            throw new UnsupportedOperationException("should not be called, use subclassed events");
        }

        public void receive(NewMessageEvent event) {
        }

        @Override
        public Receiver<MessageEvent> getInstance() {
            return null;
        }

        public void receive(RemovedMessageEvent event) {
        }

        public void receive(ModifiedMessageEvent event) {
        }
    }

    @SuppressWarnings("unchecked")
    public static class SettableRepositoryRegistry<R extends SimpleGenericRepository<?>>
            implements RepositoryRegistry {

        protected void injectInto(R rep) {
        }

        public void register(R rep) {
        }

        public void register(R... reps) {
        }

        public void setRepos(R... reps) {
        }

        @Override
        public <T> SimpleGenericRepository<T> getFor(Class<T> entityType) {
            return null;
        }

        public void afterPropertiesSet() throws Exception {
        }
    }

    public static class GenericHibernateRepository<T, ID extends Serializable>
            implements ConvenientGenericRepository<T, ID> {

        @Override
        public Class<T> getPersistentClass() {
            return null;
        }

        /**
         * @param c Mandatory. The domain class this repository is responsible for.
         */
        // Since it is impossible to determine the actual type of a type
        // parameter (!), we resort to requiring the caller to provide the
        // actual type as parameter, too.
        // Not set in a constructor to enable easy CGLIB-proxying (passing
        // constructor arguments to Spring AOP proxies is quite cumbersome).
        public void setPersistentClass(Class<T> c) {
        }

        @Override
        public T findById(ID id, boolean lock) {
            return null;
        }

        @Override
        public List<T> findAll() {
            return null;
        }

        @Override
        public List<T> findByExample(T exampleInstance) {
            return null;
        }

        @Override
        public List<T> findByQuery() {
            return null;
        }

        @Override
        public T saveOrUpdate(T entity) {
            return null;
        }

        @Override
        public void delete(T entity) {
        }

        @Override
        public T refresh(T entity) {
            return null;
        }

        @Override
        public void delete(ID id) {
        }

        @Override
        public void delete(Collection<T> entities) {
        }
    }

    public static class HibernateRepositoryRegistry
            extends SettableRepositoryRegistry<GenericHibernateRepository<?, ?>> {

        @Override
        public void injectInto(GenericHibernateRepository<?, ?> rep) {
        }

        @Override
        public <T> GenericHibernateRepository<T, ?> getFor(Class<T> entityType) {
            return null;
        }
    }

    public static class MyHomer<T extends Bounded<T>, L extends T> implements Homer<L> {

        @Override
        public void foo(L t) {
            throw new UnsupportedOperationException();
        }
    }

    public static class YourHomer<T extends AbstractBounded<T>, L extends T> extends
            MyHomer<T, L> {

        @Override
        public void foo(L t) {
            throw new UnsupportedOperationException();
        }
    }

    public static class GenericSqlMapDao<T extends Serializable> implements ConvenienceGenericDao<T> {

        @Override
        public void saveOrUpdate(T t) {
            throw new UnsupportedOperationException();
        }
    }


    //-------------------
    // SPR-3304 classes
    //-------------------

    public static class GenericSqlMapIntegerDao<T extends Number> extends GenericSqlMapDao<T> {

        @Override
        public void saveOrUpdate(T t) {
        }
    }

    public static class Permission {
    }

    public static class User {
    }

    public static abstract class AbstractDao<T> {

        public void save(T t) {
        }

        public void saveVararg(T t, Object... args) {
        }
    }

    public static class UserDaoImpl extends AbstractDao<User> implements UserDao {

        @Override
        public void save(Permission perm) {
        }

        @Override
        public void saveVararg(User user, Object... args) {
        }
    }

    public static abstract class BusinessGenericDao<T, PK extends Serializable>
            implements DaoInterface<T, PK> {

        public void save(T object) {
        }
    }

    public static class Business<T> {
    }

    public static class BusinessDao extends BusinessGenericDao<Business<?>, Long> {

        @Override
        public void save(Business<?> business) {
        }

        @Override
        public Business<?> get(Long id) {
            return null;
        }

        public Business<?> get(String code) {
            return null;
        }
    }


    //-------------------
    // SPR-3357 classes
    //-------------------

    private static class MegaEvent {
    }

    private static class MegaMessageEvent extends MegaEvent {
    }

    private static class NewMegaMessageEvent extends MegaEvent {
    }

    private static class ModifiedMegaMessageEvent extends MegaEvent {
    }

    private static class Other<S, E> {
    }


    //-------------------
    // SPR-3485 classes
    //-------------------

    @SuppressWarnings("unused")
    private static class MegaMessageProducerImpl extends Other<Long, String> implements MegaMessageProducer {

        public void receive(NewMegaMessageEvent event) {
            throw new UnsupportedOperationException();
        }

        public void receive(ModifiedMegaMessageEvent event) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void receive(MegaMessageEvent event) {
            throw new UnsupportedOperationException();
        }
    }

    private static class DomainObjectSuper {
    }

    private static class DomainObjectExtendsSuper extends DomainObjectSuper {
    }


    //-------------------
    // SPR-3534 classes
    //-------------------

    @SuppressWarnings("unused")
    private static abstract class AbstractImplementsInterface<D extends DomainObjectSuper> implements IGenericInterface<D> {

        @Override
        public <T> void doSomething(D domainObject, T value) {
        }

        public void anotherBaseMethod() {
        }
    }

    private static class ExtendsAbstractImplementsInterface extends AbstractImplementsInterface<DomainObjectExtendsSuper> {

        @Override
        public <T> void doSomething(DomainObjectExtendsSuper domainObject, T value) {
            super.doSomething(domainObject, value);
        }
    }

    @SuppressWarnings("serial")
    private static class ParameterType implements Serializable {
    }

    private static class AbstractDomainObject<P extends Serializable, R> {

        public R method1(P p) {
            return null;
        }

        public void method2(P p, R r) {
        }
    }

    private static class DomainObject extends AbstractDomainObject<ParameterType, byte[]> {

        @Override
        public byte[] method1(ParameterType p) {
            return super.method1(p);
        }

        @Override
        public void method2(ParameterType p, byte[] r) {
            super.method2(p, r);
        }
    }

    public static class SearchConditions {
    }

    public static class ExternalMessage {
    }

    public static class ExternalMessageSearchConditions<T extends ExternalMessage> extends SearchConditions {
    }

    public static class ExternalMessageProvider<S extends ExternalMessage, T extends ExternalMessageSearchConditions<S>>
            implements IExternalMessageProvider<S, T> {

        @Override
        public Collection<S> findBy(T conditions) {
            return null;
        }
    }

    public static class EmailMessage extends ExternalMessage {
    }


    //-------------------
    // SPR-16103 classes
    //-------------------

    public static class EmailSearchConditions extends ExternalMessageSearchConditions<EmailMessage> {
    }

    public static class EmailMessageProvider extends ExternalMessageProvider<EmailMessage, EmailSearchConditions> {
    }

    public static class TestEmailProvider extends EmailMessageProvider {

        @Override
        public Collection<EmailMessage> findBy(EmailSearchConditions conditions) {
            return null;
        }
    }

    public static abstract class BaseEntity {
    }

    public static class FooEntity extends BaseEntity {
    }

    public static class BaseClass<T> {

        public <S extends T> S test(S T) {
            return null;
        }
    }

    public static class EntityClass<T extends BaseEntity> extends BaseClass<T> {

        @Override
        public <S extends T> S test(S T) {
            return null;
        }
    }

    public static class FooClass extends EntityClass<FooEntity> {

        @Override
        public <S extends FooEntity> S test(S T) {
            return null;
        }
    }

}
