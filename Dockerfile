FROM rabbitmq:3.6.8-management
MAINTAINER Tuukka Lahtela tuukka.lahtela@gmail.com

RUN apt-get update && apt-get install -y iptables

COPY rabbitmq.config /etc/rabbitmq/
COPY configure_rabbit.sh /usr/local/bin/
COPY set_cookie.sh /
COPY block_broker.sh /usr/local/bin/

EXPOSE 5672 5673 5674 5675 15672 15674 15674 15675 4369 25672 25673 25674 25675
ENTRYPOINT ["/set_cookie.sh"]
CMD ["configure_rabbit.sh"]
