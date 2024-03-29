package com.BitcoinTransacGen;

import org.bitcoinj.core.*;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.wallet.SendRequest;
import org.bitcoinj.wallet.Wallet;

import java.io.File;
import java.net.InetAddress;
import java.util.List;

public class Application {

	public static void main(String[] args) throws Exception {
		// Set up the Bitcoin network parameters
		NetworkParameters params = TestNet3Params.get(); // points to org.bitcoin.test  network

		// Create the directory for wallet file storage
		File walletDir = new File("./wallet");
		if (!walletDir.exists()) {
			walletDir.mkdirs();
		}

		// Set up the wallet application kit
		WalletAppKit kit = new WalletAppKit(params, walletDir, "testnet-wallet") {
			@Override
			protected void onSetupCompleted() {
				if (wallet().getKeyChainGroupSize() < 1) {
					wallet().importKey(new ECKey());
				}
			}
		};


		// Start the wallet and sync with the blockchain
		kit.startAsync();
		kit.awaitRunning();

		System.out.println("Requesting testnet coins...");
		kit.peerGroup().addAddress(new PeerAddress(params, InetAddress.getByName("testnet-seed.bitcoin.petertodd.org")));
		kit.peerGroup().addAddress(new PeerAddress(params, InetAddress.getByName("testnet-seed.bluematt.me")));
		//kit.peerGroup().addAddress(new PeerAddress(params, InetAddress.getByName("testnet-seed.bitcoin.schildbach.de")));
		//kit.wallet().allowSpendingUnconfirmedTransactions();


		/*kit.wallet().addWatchedAddress(kit.wallet().freshReceiveAddress());
		List<Address> listAddresses = kit.wallet().getWatchedAddresses();
		for (Address a: listAddresses) {
			System.out.println("aaaaa: " + a);
		}*/

		// Print out wallet information
		Wallet wallet = kit.wallet();
		System.out.println("Current balance: " + wallet.getBalance().toFriendlyString());
		System.out.println("Receive address: " + wallet.currentReceiveAddress());
		Address testnetAddress = wallet.freshReceiveAddress();
		// Create a transaction
		Address destinationAddress = Address.fromString(params, testnetAddress.toString());
		Coin amountToSend = Coin.valueOf(10000); // Amount to send in satoshis (10000 satoshis = 0.0001 BTC)
		Transaction transaction = new Transaction(params);
		transaction.addOutput(amountToSend, destinationAddress);

		// Prepare the transaction to be sent
		SendRequest sendRequest = SendRequest.forTx(transaction);

		// Send the transaction
		wallet.completeTx(sendRequest);
		wallet.commitTx(sendRequest.tx);

		// Print out the transaction information
		System.out.println("Transaction ID: " + transaction.getHashAsString());
		System.out.println("Transaction: " + transaction.toString());
		System.out.println("Transaction sent! Waiting for confirmation...");
	}
}
