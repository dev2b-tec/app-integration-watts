package br.tec.dev2b.whats.infra.minio;

import io.minio.*;
import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MinioUploadService {

    private final MinioClient minioClient;

    @Value("${minio.bucket.conversas:conversas}")
    private String bucket;

    @Value("${minio.public-url}")
    private String minioPublicUrl;

    /**
     * Faz upload de um arquivo e retorna a URL pública.
     *
     * @param inputStream conteúdo do arquivo
     * @param contentType MIME type (ex: "image/jpeg")
     * @param extensao    extensão sem ponto (ex: "jpg")
     */
    public String upload(InputStream inputStream, String contentType, String extensao) {
        try {
            garantirBucket();
            String objectName = UUID.randomUUID() + "." + extensao;

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .stream(inputStream, -1, 10_485_760)
                            .contentType(contentType)
                            .build()
            );

            return minioPublicUrl + "/" + bucket + "/" + objectName;

        } catch (MinioException e) {
            throw new RuntimeException("Erro ao fazer upload para o MinIO: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Erro inesperado no upload: " + e.getMessage(), e);
        }
    }

    private void garantirBucket() throws Exception {
        boolean existe = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(bucket).build()
        );
        if (!existe) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
        aplicarPoliticaPublicaLeitura();
    }

    /**
     * Define política de leitura pública (s3:GetObject) no bucket.
     * Idempotente — pode ser chamado sempre, sobrescreve a policy existente.
     */
    private void aplicarPoliticaPublicaLeitura() throws Exception {
        String policy = """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Principal": {"AWS": ["*"]},
                      "Action": ["s3:GetObject"],
                      "Resource": ["arn:aws:s3:::%s/*"]
                    }
                  ]
                }
                """.formatted(bucket);

        minioClient.setBucketPolicy(
                SetBucketPolicyArgs.builder()
                        .bucket(bucket)
                        .config(policy)
                        .build()
        );
    }
}
