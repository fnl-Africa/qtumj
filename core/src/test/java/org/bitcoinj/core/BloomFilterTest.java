/*
 * Copyright 2012 Matt Corallo
 * Copyright 2014 Andreas Schildbach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bitcoinj.core;

import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.wallet.KeyChainGroup;
import org.bitcoinj.wallet.Wallet;
import org.junit.Test;

import static org.bitcoinj.core.Utils.HEX;
import static org.junit.Assert.*;

public class BloomFilterTest {
    private static final NetworkParameters MAINNET = MainNetParams.get();

    @Test
    public void insertSerializeTest() {
        BloomFilter filter = new BloomFilter(3, 0.01, 0, BloomFilter.BloomUpdate.UPDATE_ALL);

        filter.insert(HEX.decode("99108ad8ed9bb6274d3980bab5a85c048f0950c8"));
        assertTrue (filter.contains(HEX.decode("99108ad8ed9bb6274d3980bab5a85c048f0950c8")));
        // One bit different in first byte
        assertFalse(filter.contains(HEX.decode("19108ad8ed9bb6274d3980bab5a85c048f0950c8")));

        filter.insert(HEX.decode("b5a2c786d9ef4658287ced5914b37a1b4aa32eee"));
        assertTrue(filter.contains(HEX.decode("b5a2c786d9ef4658287ced5914b37a1b4aa32eee")));

        filter.insert(HEX.decode("b9300670b4c5366e95b2699e8b18bc75e5f729c5"));
        assertTrue(filter.contains(HEX.decode("b9300670b4c5366e95b2699e8b18bc75e5f729c5")));

        // Value generated by Bitcoin Core
        assertEquals("03614e9b050000000000000001", HEX.encode(filter.unsafeBitcoinSerialize()));
    }

    @Test
    public void insertSerializeTestWithTweak() {
        BloomFilter filter = new BloomFilter(3, 0.01, 2147483649L);

        filter.insert(HEX.decode("99108ad8ed9bb6274d3980bab5a85c048f0950c8"));
        assertTrue (filter.contains(HEX.decode("99108ad8ed9bb6274d3980bab5a85c048f0950c8")));
        // One bit different in first byte
        assertFalse(filter.contains(HEX.decode("19108ad8ed9bb6274d3980bab5a85c048f0950c8")));

        filter.insert(HEX.decode("b5a2c786d9ef4658287ced5914b37a1b4aa32eee"));
        assertTrue(filter.contains(HEX.decode("b5a2c786d9ef4658287ced5914b37a1b4aa32eee")));

        filter.insert(HEX.decode("b9300670b4c5366e95b2699e8b18bc75e5f729c5"));
        assertTrue(filter.contains(HEX.decode("b9300670b4c5366e95b2699e8b18bc75e5f729c5")));

        // Value generated by Bitcoin Core
        assertEquals("03ce4299050000000100008002", HEX.encode(filter.unsafeBitcoinSerialize()));
    }

    @Test
    public void walletTest() throws Exception {
        Context.propagate(new Context(MAINNET));

        DumpedPrivateKey privKey = DumpedPrivateKey.fromBase58(MAINNET, "5Kg1gnAjaLfKiwhhPpGS3QfRg2m6awQvaj98JCZBZQ5SuS2F15C");

        Address addr = LegacyAddress.fromKey(MAINNET, privKey.getKey());
        assertEquals("QT7w7ZhP9rLBwxVNiBnRYi95hRkVgdoboo", addr.toString());

        KeyChainGroup group = KeyChainGroup.builder(MAINNET).build();
        // Add a random key which happens to have been used in a recent generation
        group.importKeys(ECKey.fromPublicOnly(privKey.getKey()), ECKey.fromPublicOnly(HEX.decode("03cb219f69f1b49468bd563239a86667e74a06fcba69ac50a08a5cbc42a5808e99")));
        Wallet wallet = new Wallet(MAINNET, group);
        wallet.commitTx(new Transaction(MAINNET, HEX.decode("01000000010000000000000000000000000000000000000000000000000000000000000000ffffffff0d038754030114062f503253482fffffffff01c05e559500000000232103cb219f69f1b49468bd563239a86667e74a06fcba69ac50a08a5cbc42a5808e99ac00000000")));

        // We should have 2 per pubkey, and one for the P2PK output we have
        assertEquals(5, wallet.getBloomFilterElementCount());

        BloomFilter filter = wallet.getBloomFilter(wallet.getBloomFilterElementCount(), 0.001, 0);

        // Value generated by Bitcoin Core
        assertEquals("082ae5edc8e51d4a03080000000000000002", HEX.encode(filter.unsafeBitcoinSerialize()));
    }
}
