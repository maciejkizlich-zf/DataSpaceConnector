/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.api.multipart;

import kotlin.NotImplementedError;
import org.eclipse.dataspaceconnector.policy.model.Action;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.PolicyType;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractOfferQuery;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractOfferService;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.contract.validation.ContractValidationService;
import org.eclipse.dataspaceconnector.spi.contract.validation.OfferValidationResult;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.iam.TokenResult;
import org.eclipse.dataspaceconnector.spi.iam.VerificationResult;
import org.eclipse.dataspaceconnector.spi.message.MessageContext;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcher;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.eclipse.dataspaceconnector.spi.types.domain.message.RemoteMessage;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;

class IdsApiMultipartEndpointV1IntegrationTestServiceExtension implements ServiceExtension {
    private final List<Asset> assets;

    public IdsApiMultipartEndpointV1IntegrationTestServiceExtension(List<Asset> assets) {
        this.assets = Objects.requireNonNull(assets);
    }

    @Override
    public Set<String> provides() {
        return Set.of("edc:iam", "edc:core:contract", "dataspaceconnector:transferprocessstore", ContractDefinitionStore.FEATURE);
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        context.registerService(IdentityService.class, new FakeIdentityService());
        context.registerService(TransferProcessStore.class, new FakeTransferProcessStore());
        context.registerService(RemoteMessageDispatcherRegistry.class, new FakeRemoteMessageDispatcherRegistry());
        context.registerService(AssetIndex.class, new FakeAssetIndex(assets));
        context.registerService(ContractOfferService.class, new FakeContractOfferService(assets));
        context.registerService(ContractDefinitionStore.class, new FakeContractDefinitionStore());
        context.registerService(ContractValidationService.class, new FakeContractValidationService());
    }

    private static class FakeIdentityService implements IdentityService {
        @Override
        public TokenResult obtainClientCredentials(String scope) {
            return TokenResult.Builder.newInstance().build();
        }

        @Override
        public VerificationResult verifyJwtToken(String token, String audience) {
            return new VerificationResult(ClaimToken.Builder.newInstance().build());
        }
    }

    private static class FakeAssetIndex implements AssetIndex {
        private final List<Asset> assets;

        private FakeAssetIndex(List<Asset> assets) {
            this.assets = Objects.requireNonNull(assets);
        }

        @Override
        public Stream<Asset> queryAssets(AssetSelectorExpression expression) {
            return assets.stream();
        }

        @Override
        public Asset findById(String assetId) {
            return assets.stream().filter(a -> a.getId().equals(assetId)).findFirst().orElse(null);
        }
    }

    private static class FakeContractOfferService implements ContractOfferService {
        private final List<Asset> assets;

        private FakeContractOfferService(List<Asset> assets) {
            this.assets = assets;
        }

        @Override
        @NotNull
        public Stream<ContractOffer> queryContractOffers(ContractOfferQuery query) {
            List<ContractOffer> contractOffers = assets.stream()
                    .map(Collections::singletonList)
                    .map(assets -> ContractOffer.Builder.newInstance()
                            .policy(createEverythingAllowedPolicy())
                            .id("1")
                            .assets(assets).build())
                    .collect(Collectors.toList());
            return contractOffers.stream();
        }

        private Policy createEverythingAllowedPolicy() {
            var policyBuilder = Policy.Builder.newInstance();
            var permissionBuilder = Permission.Builder.newInstance();
            var actionBuilder = Action.Builder.newInstance();

            policyBuilder.type(PolicyType.CONTRACT);
            actionBuilder.type("USE");

            permissionBuilder.action(actionBuilder.build());
            policyBuilder.permission(permissionBuilder.build());
            return policyBuilder.build();
        }
    }

    private static class FakeTransferProcessStore implements TransferProcessStore {
        @Override
        public TransferProcess find(String id) {
            return null;
        }

        @Override
        public @Nullable String processIdForTransferId(String id) {
            return null;
        }

        @Override
        public @NotNull List<TransferProcess> nextForState(int state, int max) {
            return emptyList();
        }

        @Override
        public void create(TransferProcess process) {
        }

        @Override
        public void update(TransferProcess process) {
        }

        @Override
        public void delete(String processId) {
        }

        @Override
        public void createData(String processId, String key, Object data) {
        }

        @Override
        public void updateData(String processId, String key, Object data) {
        }

        @Override
        public void deleteData(String processId, String key) {
        }

        @Override
        public void deleteData(String processId, Set<String> keys) {
        }

        @Override
        public <T> T findData(Class<T> type, String processId, String resourceDefinitionId) {
            return null;
        }
    }

    private static class FakeRemoteMessageDispatcherRegistry implements RemoteMessageDispatcherRegistry {

        @Override
        public void register(RemoteMessageDispatcher dispatcher) {
        }

        @Override
        public <T> CompletableFuture<T> send(Class<T> responseType, RemoteMessage message, MessageContext context) {
            return null;
        }
    }

    private static class FakeContractDefinitionStore implements ContractDefinitionStore {

        private final List<ContractDefinition> contractDefinitions = new ArrayList<>();

        @Override
        public @NotNull Collection<ContractDefinition> findAll() {
            return contractDefinitions;
        }

        @Override
        public void save(Collection<ContractDefinition> definitions) {
            contractDefinitions.addAll(definitions);
        }

        @Override
        public void save(ContractDefinition definition) {
            contractDefinitions.add(definition);
        }

        @Override
        public void update(ContractDefinition definition) {
            throw new NotImplementedError();
        }

        @Override
        public void delete(String id) {
            throw new NotImplementedError();
        }

        @Override
        public void reload() {
            throw new NotImplementedError();
        }
    }

    private static class FakeContractValidationService implements ContractValidationService {

        @Override
        public @NotNull OfferValidationResult validate(ClaimToken token, ContractOffer offer) {
            return new OfferValidationResult(ContractOffer.Builder.newInstance().build());
        }

        @Override
        public @NotNull OfferValidationResult validate(ClaimToken token, ContractOffer offer, ContractOffer latestOffer) {
            return null;
        }

        @Override
        public boolean validate(ClaimToken token, ContractAgreement agreement, ContractOffer latestOffer) {
            return false;
        }

        @Override
        public boolean validate(ClaimToken token, ContractAgreement agreement) {
            return true;
        }
    }
}