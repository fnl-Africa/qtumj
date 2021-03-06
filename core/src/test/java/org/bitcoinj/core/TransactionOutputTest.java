/*
 * Copyright by the original author or authors.
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

import com.google.common.collect.ImmutableList;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.script.ScriptPattern;
import org.bitcoinj.testing.TestWithWallet;
import org.bitcoinj.wallet.SendRequest;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class TransactionOutputTest extends TestWithWallet {

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testMultiSigOutputToString() throws Exception {
        sendMoneyToWallet(AbstractBlockChain.NewBlockType.BEST_CHAIN, Coin.COIN);
        ECKey myKey = new ECKey();
        this.wallet.importKey(myKey);

        // Simulate another signatory
        ECKey otherKey = new ECKey();

        // Create multi-sig transaction
        Transaction multiSigTransaction = new Transaction(UNITTEST);
        ImmutableList<ECKey> keys = ImmutableList.of(myKey, otherKey);

        Script scriptPubKey = ScriptBuilder.createMultiSigOutputScript(2, keys);
        multiSigTransaction.addOutput(Coin.COIN, scriptPubKey);

        SendRequest req = SendRequest.forTx(multiSigTransaction);
        this.wallet.completeTx(req);
        TransactionOutput multiSigTransactionOutput = multiSigTransaction.getOutput(0);

        assertThat(multiSigTransactionOutput.toString(), CoreMatchers.containsString("CHECKMULTISIG"));
    }

    @Test
    public void testP2SHOutputScript() throws Exception {
        String P2SHAddressString = "MUWEsZbNQiMXqxHAifTVCm7hK69vLHU42a";
        Address P2SHAddress = LegacyAddress.fromBase58(MAINNET, P2SHAddressString);
        Script script = ScriptBuilder.createOutputScript(P2SHAddress);
        Transaction tx = new Transaction(MAINNET);
        tx.addOutput(Coin.COIN, script);
        assertEquals(P2SHAddressString, tx.getOutput(0).getScriptPubKey().getToAddress(MAINNET).toString());
    }

    @Test
    public void getAddressTests() throws Exception {
        Transaction tx = new Transaction(MAINNET);
        tx.addOutput(Coin.CENT, ScriptBuilder.createOpReturnScript("hello world!".getBytes()));
        assertTrue(ScriptPattern.isOpReturn(tx.getOutput(0).getScriptPubKey()));
        assertFalse(ScriptPattern.isP2PK(tx.getOutput(0).getScriptPubKey()));
        assertFalse(ScriptPattern.isP2PKH(tx.getOutput(0).getScriptPubKey()));
    }
    
    @Test
    public void testContractCreate() {
        final byte[] bytes = "dummy bytes".getBytes();
        Transaction tx = new Transaction(MAINNET);
        
        tx.addOpCreateOutput(bytes, 20000L, 50L);
        final Script scriptPubkey0 = tx.getOutput(0).getScriptPubKey();
        assertTrue(ScriptPattern.isOpCreate(scriptPubkey0));
        assertFalse(ScriptPattern.isOpCall(scriptPubkey0));
        assertEquals(20000L, scriptPubkey0.getGasLimit());
        assertEquals(50L, scriptPubkey0.getGasPrice());

        tx.addOpCreateOutput(bytes);
        final Script scriptPubkey1 = tx.getOutput(1).getScriptPubKey();
        assertTrue(ScriptPattern.isOpCreate(scriptPubkey1));
        assertFalse(ScriptPattern.isOpCall(scriptPubkey1));
        assertEquals(250000L, scriptPubkey1.getGasLimit());
        assertEquals(40L, scriptPubkey1.getGasPrice());

        assertTrue(ScriptPattern.isContract(scriptPubkey0));
        assertTrue(ScriptPattern.isContract(scriptPubkey1));
    }
    
    @Test
    public void testContractCall() {
        final byte[] bytes = "dummy bytes".getBytes();
        final ContractAddress addr = ContractAddress.fromString("0000000000000000000000000000000000000000");
        Transaction tx = new Transaction(MAINNET);
        
        tx.addOpCallOutput(bytes, addr, 3721087L, 456L);
        final Script scriptPubkey0 = tx.getOutput(0).getScriptPubKey();
        assertFalse(ScriptPattern.isOpCreate(scriptPubkey0));
        assertTrue(ScriptPattern.isOpCall(scriptPubkey0));
        assertEquals(3721087L, scriptPubkey0.getGasLimit());
        assertEquals(456L, scriptPubkey0.getGasPrice());
        
        tx.addOpCallOutput(bytes, addr);
        final Script scriptPubkey1 = tx.getOutput(1).getScriptPubKey();
        assertFalse(ScriptPattern.isOpCreate(scriptPubkey1));
        assertTrue(ScriptPattern.isOpCall(scriptPubkey1));
        assertEquals(250000L, scriptPubkey1.getGasLimit());
        assertEquals(40L, scriptPubkey1.getGasPrice());

        assertTrue(ScriptPattern.isContract(scriptPubkey0));
        assertTrue(ScriptPattern.isContract(scriptPubkey1));
    }

    @Test
    public void getMinNonDustValue() throws Exception {
        TransactionOutput p2pk = new TransactionOutput(UNITTEST, null, Coin.COIN, myKey);
        assertEquals(Coin.valueOf(76800), p2pk.getMinNonDustValue());
        TransactionOutput p2pkh = new TransactionOutput(UNITTEST, null, Coin.COIN, LegacyAddress.fromKey(UNITTEST, myKey));
        assertEquals(Coin.valueOf(72800), p2pkh.getMinNonDustValue());
        TransactionOutput p2wpkh = new TransactionOutput(UNITTEST, null, Coin.COIN, SegwitAddress.fromKey(UNITTEST, myKey));
        assertEquals(Coin.valueOf(39200), p2wpkh.getMinNonDustValue());
    }
}
