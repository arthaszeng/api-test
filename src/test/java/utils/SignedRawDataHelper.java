package utils;

import common.Region;
import org.jetbrains.annotations.NotNull;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jService;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.http.HttpService;
import org.web3j.protocol.ipc.UnixIpcService;
import org.web3j.protocol.ipc.WindowsIpcService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.utils.Async;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;

public class SignedRawDataHelper {
    private final static String RPS_URL = "http://node1.quorum.cn.blockchain.thoughtworks.cn:80";
    private final static String CUSTOMER_PRIVATE_KEY = "53ae418dd7bcd31513f47ec6b816b3926508d2163939e241c38208246e4397f4";
    private final static String MERCHANT_PRIVATE_KEY = "e9cc95e0bc6893cb195beafc7b3f43690fd402059418700be96de2084fa4f25b";
    private final static String CONTRACT_ADDRESS = "0x32862a1861cE3AABe043f47c672ee26b9244F613";
    private final static String ROC_MACAU_ADDRESS_DEV = "0x395E9294991086eDC9fce644197Ac244b30768F9";
    private final static String ROC_MANILA_ADDRESS_PROD = "0x5063D554cED7F296315aA49f8d9a02F466696De1";

    private final static long GAS_LIMIT = 450000000L;

    private final static Web3j web3j = quorum();

    private static Web3j quorum() {
        String nodeEndpoint = RPS_URL;
        Web3jService web3jService;

        if (nodeEndpoint == null || nodeEndpoint.equals("")) {
            web3jService = new HttpService();
        } else if (nodeEndpoint.startsWith("http")) {
            web3jService = new HttpService(nodeEndpoint);
        } else if (System.getProperty("os.name").toLowerCase().startsWith("win")) {
            web3jService = new WindowsIpcService(nodeEndpoint);
        } else {
            web3jService = new UnixIpcService(nodeEndpoint);
        }

        return Web3j.build(web3jService, 1000L, Async.defaultExecutorService());
    }

    public static String getSpendSignedRawTransaction(String merchantAddress, int points) throws IOException {

        Credentials credential = Credentials.create(CUSTOMER_PRIVATE_KEY);

        RawTransactionManager rawTransactionManager =
                new RawTransactionManager(web3j, credential);

        Function function = getSpendFunction(merchantAddress, points);
        String encode = FunctionEncoder.encode(function);

        EthGetTransactionCount ethGetTransactionCount =
                web3j.ethGetTransactionCount(credential.getAddress(), DefaultBlockParameterName.PENDING).send();

        BigInteger nonce = ethGetTransactionCount.getTransactionCount();

        RawTransaction rawTransaction =
                RawTransaction.createTransaction(nonce, BigInteger.ZERO, BigInteger.valueOf(GAS_LIMIT), CONTRACT_ADDRESS, encode);

        return rawTransactionManager.sign(rawTransaction);
    }

    @NotNull
    private static Function getSpendFunction(String merchantAddress, int points) {
        return new Function(
                "spendPoints",
                Arrays.asList(new org.web3j.abi.datatypes.Address(merchantAddress),
                        new org.web3j.abi.datatypes.generated.Uint256(points)),
                Collections.emptyList());
    }

    public static String getRedeemSignedRawTransaction(int points, Region region) throws IOException {

        Credentials credential = Credentials.create(MERCHANT_PRIVATE_KEY);

        RawTransactionManager rawTransactionManager =
                new RawTransactionManager(web3j, credential);

        Function function = getRedeemForMerchantFunction(points, region);
        String encode = FunctionEncoder.encode(function);

        EthGetTransactionCount ethGetTransactionCount =
                web3j.ethGetTransactionCount(credential.getAddress(), DefaultBlockParameterName.PENDING).send();

        BigInteger nonce = ethGetTransactionCount.getTransactionCount();

        RawTransaction rawTransaction =
                RawTransaction.createTransaction(nonce, BigInteger.ZERO, BigInteger.valueOf(GAS_LIMIT), CONTRACT_ADDRESS, encode);

        return rawTransactionManager.sign(rawTransaction);
    }

    @NotNull
    private static Function getRedeemForMerchantFunction(int points, Region region) {
        if (region.equals(Region.MACAU)) {
            return new Function(
                    "redeemPointsForMerchant",
                    Arrays.asList(new org.web3j.abi.datatypes.Address(ROC_MACAU_ADDRESS_DEV),
                            new org.web3j.abi.datatypes.generated.Uint256(points)),
                    Collections.emptyList());
        }

        if (region.equals(Region.MANILA)) {
            return new Function(
                    "redeemPointsForMerchant",
                    Arrays.asList(new org.web3j.abi.datatypes.Address(ROC_MANILA_ADDRESS_PROD),
                            new org.web3j.abi.datatypes.generated.Uint256(points)),
                    Collections.emptyList());
        }
        return null;
    }

}
