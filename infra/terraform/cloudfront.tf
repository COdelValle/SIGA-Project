# CloudFront delante de la EC2 para servir el SPA por HTTPS.
#
# Motivo: MSAL usa Web Crypto (crypto.subtle), que el navegador solo expone en
# contextos seguros (HTTPS o localhost). Sirviendo por HTTP desde la IP publica
# falla con "crypto_nonexistent". CloudFront aporta HTTPS con su certificado
# por defecto (*.cloudfront.net), sin necesidad de dominio propio ni ACM.
#
# El API Gateway se sigue llamando directo desde el navegador (no pasa por aqui).
resource "aws_cloudfront_distribution" "spa" {
  enabled             = true
  is_ipv6_enabled     = true
  default_root_object = "index.html"
  price_class         = "PriceClass_100"
  comment             = "${var.management_name} SPA"

  origin {
    domain_name = aws_eip.app.public_dns
    origin_id   = "siga-ec2"

    custom_origin_config {
      http_port              = 80
      https_port             = 443
      origin_protocol_policy = "http-only"
      origin_ssl_protocols   = ["TLSv1.2"]
    }
  }

  default_cache_behavior {
    target_origin_id       = "siga-ec2"
    viewer_protocol_policy = "redirect-to-https"
    allowed_methods        = ["GET", "HEAD", "OPTIONS"]
    cached_methods         = ["GET", "HEAD"]
    compress               = true

    # Sin cache y reenviando query string: el callback OAuth llega como
    # /auth?code=...&state=... y no debe perderse ni quedar cacheado.
    forwarded_values {
      query_string = true
      cookies {
        forward = "none"
      }
    }

    min_ttl     = 0
    default_ttl = 0
    max_ttl     = 0
  }

  # Fallback de SPA por si nginx devuelve 403/404 en rutas profundas.
  custom_error_response {
    error_code         = 403
    response_code      = 200
    response_page_path = "/index.html"
  }

  custom_error_response {
    error_code         = 404
    response_code      = 200
    response_page_path = "/index.html"
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  viewer_certificate {
    cloudfront_default_certificate = true
  }

  tags = {
    Name = "${var.management_name}-cloudfront"
  }
}
