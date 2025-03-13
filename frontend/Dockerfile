FROM node:22 AS build

WORKDIR /app

RUN npm install -g @angular/cli
COPY package.json package-lock.json ./
RUN npm install
COPY . .
RUN ng build --configuration production

FROM nginx:alpine
COPY --from=build /app/dist/cloud-thrash/browser /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
