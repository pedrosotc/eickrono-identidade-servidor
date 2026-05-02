package:
	mvn -q -DskipTests package

test:
	EICKRONO_TEST_PREFER_LOCAL_POSTGRES=true mvn -q test

test-rapido:
	EICKRONO_TEST_PREFER_LOCAL_POSTGRES=true mvn -q -Dtest=ProvisionamentoIdentidadeServiceTest,RecuperacaoSenhaServiceTest,VinculoSocialServiceTest test
