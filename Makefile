package:
	mvn -q -DskipTests package

test:
	mvn -q test

test-rapido:
	mvn -q -Dtest=ProvisionamentoIdentidadeServiceTest,RecuperacaoSenhaServiceTest,VinculoSocialServiceTest test
