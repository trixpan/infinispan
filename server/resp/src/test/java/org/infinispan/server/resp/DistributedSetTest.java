package org.infinispan.server.resp;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.infinispan.functional.FunctionalTestUtils.await;
import static org.infinispan.server.resp.test.RespTestingUtil.ELAIA;
import static org.infinispan.server.resp.test.RespTestingUtil.FELIX;
import static org.infinispan.server.resp.test.RespTestingUtil.NAMES_KEY;
import static org.infinispan.server.resp.test.RespTestingUtil.OIHANA;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.distribution.BaseDistFunctionalTest;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.multimap.impl.EmbeddedSetCache;
import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.remoting.transport.Address;
import org.infinispan.server.resp.test.MultimapSCIImpl;
import org.infinispan.test.data.Person;
import org.testng.annotations.Test;

@Test(groups = "functional", testName = "distribution.DistributedSetTest")
public class DistributedSetTest extends BaseDistFunctionalTest<String, Collection<Person>> {

   protected Map<Address, EmbeddedSetCache<String, Person>> listCluster = new HashMap<>();
   protected boolean fromOwner;

   public DistributedSetTest fromOwner(boolean fromOwner) {
      this.fromOwner = fromOwner;
      return this;
   }

   @Override
   protected void createCacheManagers() throws Throwable {
      super.createCacheManagers();

      for (EmbeddedCacheManager cm : cacheManagers) {
         listCluster.put(cm.getAddress(), new EmbeddedSetCache<String, Person>(cm.getCache(cacheName)));
      }
   }

   @Override
   protected SerializationContextInitializer getSerializationContext() {
      return MultimapSCIImpl.INSTANCE;
   }

   @Override
   protected String[] parameterNames() {
      return concat(super.parameterNames(), "fromOwner");
   }

   @Override
   protected Object[] parameterValues() {
      return concat(super.parameterValues(), fromOwner ? Boolean.TRUE : Boolean.FALSE);
   }

   @Override
   public Object[] factory() {
      return new Object[] {
            new DistributedSetTest().fromOwner(false).cacheMode(CacheMode.DIST_SYNC).transactional(false),
            new DistributedSetTest().fromOwner(true).cacheMode(CacheMode.DIST_SYNC).transactional(false),
      };
   }

   @Override
   protected void initAndTest() {
      for (var set : listCluster.values()) {
         assertThat(await(set.size(NAMES_KEY))).isEqualTo(0L);
      }
   }

   protected EmbeddedSetCache<String, Person> getSetCacheMember() {
      return listCluster.values().stream().findFirst().orElseThrow(() -> new IllegalStateException("Cluster is empty"));
   }

   @Test
   public void testOfferFirstAndLast() {
      initAndTest();
      EmbeddedSetCache<String, Person> set = getSetCacheMember();
      await(set.add(NAMES_KEY, OIHANA));
      assertValuesAndOwnership(NAMES_KEY, OIHANA);

      await(set.add(NAMES_KEY, ELAIA));
      assertValuesAndOwnership(NAMES_KEY, ELAIA);

   }

   @Test
   public void testSize() {
      initAndTest();
      EmbeddedSetCache<String, Person> set = getSetCacheMember();
      await(
            set.add(NAMES_KEY, OIHANA)
                  .thenCompose(r1 -> set.add(NAMES_KEY, ELAIA))
                  .thenCompose(r1 -> set.add(NAMES_KEY, FELIX))
                  .thenCompose(r1 -> set.add(NAMES_KEY, OIHANA))
                  .thenCompose(r1 -> set.size(NAMES_KEY))
                  .thenAccept(size -> assertThat(size).isEqualTo(3))
      );
   }

   protected void assertValuesAndOwnership(String key, Person value) {
      assertOwnershipAndNonOwnership(key, l1CacheEnabled);
      assertOnAllCaches(key, value);
   }

   protected void assertOnAllCaches(Object key, Person value) {
      for (Map.Entry<Address, EmbeddedSetCache<String, Person>> entry : listCluster.entrySet()) {
         await(entry.getValue().get((String) key).thenAccept(v -> {
            assertNotNull(format("values on the key %s must be not null", key), v);
            assertTrue(format("values on the key '%s' must contain '%s' on node '%s'", key, value, entry.getKey()),
                  v.contains(value));
         })

         );
      }
   }
}
