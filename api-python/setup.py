import setuptools

with open("README.md","r") as fh:
    long_description = fh.read()

setuptools.setup(name='sepy',
      version='0.42',
      description=' Client-side libraries for the SEPA platform (Python3) ',
      long_description=long_description,
      long_description_content_type="text/markdown",
      url='https://github.com/fr4ncidir/SEPA-python3-APIs',
      author='Fabio Viola, Francesco Antoniazzi',
      author_email='fabio.viola@unibo.it, francesco.antoniazzi1991@gmail.com',
      packages=setuptools.find_packages(),
      license='GPLv3',
      test_suite="sepy.tests",
      install_requires=[
          "websocket-client",
          "argparse",
          "prettytable",
          "pyyaml",
          "jinja2"
      ],
      include_package_data=True,
      zip_safe=False)
