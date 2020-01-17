# Python support can be specified down to the minor or micro version
# (e.g. 3.6 or 3.6.3).
# OS Support also exists for jessie & stretch (slim and full).
# See https://hub.docker.com/r/library/python/ for all supported Python
# tags from Docker Hub.
FROM python:3

LABEL Name=p3g2-opencv Version=0.0.3

WORKDIR /opencv
COPY requirements.txt /opencv
COPY opencv_filter.py /opencv

# Using pip:
RUN python3 -m pip install -r requirements.txt
ENTRYPOINT ["python3", "opencv_filter.py"]
 